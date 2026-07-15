package com.marcura.exchangerate.service.impl;

import com.marcura.exchangerate.domain.CurrencyUsage;
import com.marcura.exchangerate.domain.ExchangeRate;
import com.marcura.exchangerate.exception.RateNotFoundException;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import com.marcura.exchangerate.service.ExchangeRateService;
import com.marcura.exchangerate.service.SpreadCalculator;
import com.marcura.exchangerate.web.dto.ExchangeResponse;
import com.marcura.exchangerate.web.dto.HistoricalRatesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository rateRepository;
    private final CurrencyUsageRepository usageRepository;
    private final SpreadCalculator spreadCalculator;

    @Override
    @Transactional
    public ExchangeResponse getExchange(String from, String to, LocalDate date) {
        String fromCcy = from.toUpperCase();
        String toCcy = to.toUpperCase();

        LocalDate effectiveDate = date != null
                ? date
                : rateRepository.findLatestRateDate()
                        .orElseThrow(() -> new RateNotFoundException("No exchange rates available yet"));

        ExchangeRate fromRate = requireRate(fromCcy, effectiveDate);
        ExchangeRate toRate = requireRate(toCcy, effectiveDate);

        BigDecimal exchange = spreadCalculator.spreadAdjustedRate(
                fromCcy, toCcy, fromRate.getRate(), toRate.getRate(), fromRate.getBaseCurrency());

        // Both currencies of a successful query are counted, atomically.
        long fromCount = incrementUsage(fromCcy, effectiveDate);
        long toCount = incrementUsage(toCcy, effectiveDate);

        return new ExchangeResponse(fromCcy, toCcy, exchange, effectiveDate, fromCount, toCount);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoricalRatesResponse getHistorical(String from, String to, LocalDate fromDate, LocalDate toDate) {
        String fromCcy = from.toUpperCase();
        String toCcy = to.toUpperCase();

        Map<LocalDate, ExchangeRate> fromByDate = indexByDate(
                rateRepository.findByCurrencyCodeAndRateDateBetweenOrderByRateDateAsc(fromCcy, fromDate, toDate));
        Map<LocalDate, ExchangeRate> toByDate = indexByDate(
                rateRepository.findByCurrencyCodeAndRateDateBetweenOrderByRateDateAsc(toCcy, fromDate, toDate));

        // Only dates where both currencies have a rate produce a comparable point.
        List<HistoricalRatesResponse.Point> points = new TreeMap<>(fromByDate).entrySet().stream()
                .filter(e -> toByDate.containsKey(e.getKey()))
                .map(e -> {
                    ExchangeRate f = e.getValue();
                    ExchangeRate t = toByDate.get(e.getKey());
                    BigDecimal rate = spreadCalculator.spreadAdjustedRate(
                            fromCcy, toCcy, f.getRate(), t.getRate(), f.getBaseCurrency());
                    return new HistoricalRatesResponse.Point(e.getKey(), rate);
                })
                .toList();

        // Historical browsing does not affect usage counters (only /exchange queries do).
        return new HistoricalRatesResponse(fromCcy, toCcy, fromDate, toDate, points);
    }

    private ExchangeRate requireRate(String currency, LocalDate date) {
        return rateRepository.findByCurrencyCodeAndRateDate(currency, date)
                .orElseThrow(() -> new RateNotFoundException(
                        "No rate for %s on %s".formatted(currency, date)));
    }

    /**
     * Atomic, race-free increment: a single UPDATE guarded by the DB. Counter rows are pre-created when
     * rates are stored (seeder / collection), so the hot path is a pure UPDATE. The lazy insert below is
     * only a safety net for a currency that has a rate but somehow no counter row yet.
     */
    private long incrementUsage(String currency, LocalDate date) {
        if (usageRepository.incrementUsage(currency, date) == 0) {
            usageRepository.saveAndFlush(new CurrencyUsage(currency));
            usageRepository.incrementUsage(currency, date);
        }
        return usageRepository.findById(currency).map(CurrencyUsage::getQueryCount).orElse(0L);
    }

    private Map<LocalDate, ExchangeRate> indexByDate(List<ExchangeRate> rates) {
        return rates.stream().collect(Collectors.toMap(
                ExchangeRate::getRateDate, Function.identity(), (a, b) -> b));
    }
}
