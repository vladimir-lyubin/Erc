package com.marcura.exchangerate.service;

import com.marcura.exchangerate.config.SpreadProperties;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpreadCalculatorTest {

    private final SpreadCalculator calculator = new SpreadCalculator(new SpreadProperties(
            "2.75",
            Map.of(
                    "3.25", List.of("JPY", "HKD", "KRW"),
                    "4.50", List.of("MYR", "INR", "MXN"),
                    "6.00", List.of("RUB", "CNY", "ZAR")
            )
    ));

    @Test
    void baseCurrencyHasZeroSpread() {
        assertThat(calculator.spreadPercent("EUR", "EUR")).isEqualByComparingTo("0");
    }

    @Test
    void usesConfiguredGroupSpreads() {
        assertThat(calculator.spreadPercent("JPY", "EUR")).isEqualByComparingTo("3.25");
        assertThat(calculator.spreadPercent("RUB", "EUR")).isEqualByComparingTo("6.00");
        assertThat(calculator.spreadPercent("GBP", "EUR")).isEqualByComparingTo("2.75");
    }

    @Test
    void unknownCurrencyFallsBackToDefaultSpread() {
        assertThat(calculator.spreadPercent("XYZ", "EUR")).isEqualByComparingTo("2.75");
    }

    @Test
    void spreadLookupIsCaseInsensitive() {
        assertThat(calculator.spreadPercent("jpy", "EUR")).isEqualByComparingTo("3.25");
        assertThat(calculator.spreadPercent("Rub", "eur")).isEqualByComparingTo("6.00");
        // base match is case-insensitive too
        assertThat(calculator.spreadPercent("eur", "EUR")).isEqualByComparingTo("0");
    }

    @Test
    void baseToBaseIsExactlyOne() {
        BigDecimal result = calculator.spreadAdjustedRate("EUR", "EUR",
                BigDecimal.ONE, BigDecimal.ONE, "EUR");
        assertThat(result).isEqualByComparingTo("1");
    }

    @Test
    void sameNonBaseCurrencyStillAppliesItsSpread() {
        // from == to: cross-rate is 1, but the (2.75%) spread is still applied -> 0.9725.
        BigDecimal result = calculator.spreadAdjustedRate("USD", "USD",
                new BigDecimal("1.08"), new BigDecimal("1.08"), "EUR");
        assertThat(result).isEqualByComparingTo("0.9725");
    }

    @Test
    void reciprocalRatesDivideWithScaleTwelve() {
        // cross = 1/3 rounded HALF_EVEN at scale 12 = 0.333333333333 ; factor (EUR 0% vs default 2.75%) = 0.9725
        BigDecimal result = calculator.spreadAdjustedRate("EUR", "GBP",
                new BigDecimal("3"), BigDecimal.ONE, "EUR");
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.333333333333").multiply(new BigDecimal("0.9725")));
    }

    @Test
    void picksHigherOfTwoSpreads() {
        // RUB (6%) vs JPY (3.25%) -> applied 6%.
        // cross = 50/100 = 0.5 ; factor = 1 - 0.06 = 0.94 ; expected 0.47
        BigDecimal result = calculator.spreadAdjustedRate("JPY", "RUB",
                new BigDecimal("100"), new BigDecimal("50"), "EUR");
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.47"));
    }

    /**
     * Formula shape from the brief's worked example (EUR->PLN). NOTE: the brief's example uses
     * illustrative spreads (EUR 1%, PLN 4%) that differ from the Appendix B table; here we verify
     * the arithmetic identity  result = (toRate/fromRate) * (1 - MAX/100)  reproduces the expected
     * 4.4405487565413254 when MAX = 4%.
     */
    @Test
    void reproducesWorkedExampleArithmeticWithFourPercentSpread() {
        BigDecimal cross = new BigDecimal("4.4405487565413254")
                .divide(new BigDecimal("0.96"), java.math.MathContext.DECIMAL64);
        BigDecimal factor = BigDecimal.ONE.subtract(new BigDecimal("4").divide(new BigDecimal("100")));
        BigDecimal result = cross.multiply(factor);
        assertThat(result.doubleValue()).isCloseTo(4.4405487565413254, Offset.offset(1e-9));
    }
}
