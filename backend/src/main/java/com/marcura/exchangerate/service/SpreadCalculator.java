package com.marcura.exchangerate.service;

import com.marcura.exchangerate.config.SpreadProperties;
import com.marcura.exchangerate.util.CurrencyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure spread-adjusted rate calculation. No persistence, no Spring beyond config injection,
 * so it is trivially unit-testable.
 *
 * <pre>
 * rate(from,to) = (toRatePerBase / fromRatePerBase) * (1 - MAX(spread(from), spread(to)) / 100)
 * </pre>
 *
 * The higher of the two currencies' spreads is applied. The base currency has a 0% spread.
 */
@Component
public class SpreadCalculator {

    private static final int DIVISION_SCALE = 12;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal defaultSpread;
    private final Map<String, BigDecimal> currencyToSpread = new HashMap<>();

    public SpreadCalculator(SpreadProperties properties) {
        this.defaultSpread = new BigDecimal(properties.defaultPercent());
        properties.groups().forEach((percent, currencies) -> {
            BigDecimal spread = new BigDecimal(percent);
            currencies.forEach(code -> currencyToSpread.put(CurrencyUtils.normalize(code), spread));
        });
    }

    /** Spread percent for a single currency, taking the configured base currency as 0%. */
    public BigDecimal spreadPercent(String currency, String baseCurrency) {
        if (currency.equalsIgnoreCase(baseCurrency)) {
            return BigDecimal.ZERO;
        }
        return currencyToSpread.getOrDefault(CurrencyUtils.normalize(currency), defaultSpread);
    }

    /**
     * @param fromRatePerBase rate of the source currency relative to the stored base
     * @param toRatePerBase   rate of the target currency relative to the stored base
     */
    public BigDecimal spreadAdjustedRate(String from,
                                         String to,
                                         BigDecimal fromRatePerBase,
                                         BigDecimal toRatePerBase,
                                         String baseCurrency) {
        BigDecimal appliedSpread = spreadPercent(from, baseCurrency)
                .max(spreadPercent(to, baseCurrency));

        BigDecimal crossRate = toRatePerBase.divide(fromRatePerBase, DIVISION_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal factor = BigDecimal.ONE.subtract(appliedSpread.divide(HUNDRED, DIVISION_SCALE, RoundingMode.HALF_EVEN));

        return crossRate.multiply(factor);
    }
}
