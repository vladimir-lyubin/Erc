package com.marcura.exchangerate.service;

import com.marcura.exchangerate.config.AppProperties;
import com.marcura.exchangerate.domain.CurrencyUsage;
import com.marcura.exchangerate.domain.ExchangeRate;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Seeds a deterministic history of synthetic rates on first run, so the frontend chart and analytics
 * have data even without a live Fixer.io key (the free plan has no historical endpoint).
 * Controlled by {@code app.seed.enabled} / {@code app.seed.days}; skipped when data already exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private static final String BASE = "EUR";
    // Main currencies only (per Q5). Start values are approximate EUR-based rates.
    private static final Map<String, Double> START_RATES = Map.of(
            "USD", 1.08, "GBP", 0.85, "AED", 3.96);

    private final AppProperties properties;
    private final ExchangeRateRepository rateRepository;
    private final CurrencyUsageRepository usageRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.seed().enabled() || rateRepository.count() > 0) {
            return;
        }
        int days = properties.seed().days();
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        Random random = new Random(42); // deterministic

        List<ExchangeRate> rates = IntStream.range(0, days)
                .mapToObj(startDate::plusDays)
                .flatMap(date -> ratesForDay(date, startDate, random))
                .toList();
        rateRepository.saveAll(rates);

        // Pre-create usage counters so /exchange increments are a pure atomic UPDATE.
        List<CurrencyUsage> counters = Stream.concat(Stream.of(BASE), START_RATES.keySet().stream())
                .map(CurrencyUsage::new)
                .toList();
        usageRepository.saveAll(counters);

        log.info("Seeded {} synthetic rate rows across {} days", rates.size(), days);
    }

    /** Rates for a single day: the base currency at 1, plus a synthetic random-walk value per currency. */
    private Stream<ExchangeRate> ratesForDay(LocalDate date, LocalDate startDate, Random random) {
        long dayOffset = ChronoUnit.DAYS.between(startDate, date);
        Stream<ExchangeRate> baseRate = Stream.of(new ExchangeRate(BASE, BigDecimal.ONE, date, BASE));
        Stream<ExchangeRate> currencyRates = START_RATES.entrySet().stream()
                .map(entry -> {
                    double drift = 1 + (random.nextDouble() - 0.5) * 0.02; // +/-1% daily walk
                    double value = entry.getValue() * Math.pow(drift, dayOffset + 1d);
                    BigDecimal rate = BigDecimal.valueOf(value).setScale(12, RoundingMode.HALF_EVEN);
                    return new ExchangeRate(entry.getKey(), rate, date, BASE);
                });
        return Stream.concat(baseRate, currencyRates);
    }
}
