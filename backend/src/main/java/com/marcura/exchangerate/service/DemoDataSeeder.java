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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        Random random = new Random(42); // deterministic
        List<ExchangeRate> batch = new ArrayList<>();

        for (int d = 0; d < days; d++) {
            final int day = d;
            LocalDate date = start.plusDays(day);
            batch.add(new ExchangeRate(BASE, BigDecimal.ONE, date, BASE));
            START_RATES.forEach((code, startRate) -> {
                double drift = 1 + (random.nextDouble() - 0.5) * 0.02; // +/-1% daily walk
                double value = startRate * Math.pow(drift, day + 1d);
                batch.add(new ExchangeRate(code,
                        BigDecimal.valueOf(value).setScale(12, RoundingMode.HALF_EVEN), date, BASE));
            });
        }
        rateRepository.saveAll(batch);

        // Pre-create usage counters so /exchange increments are a pure atomic UPDATE.
        List<CurrencyUsage> counters = new ArrayList<>();
        counters.add(new CurrencyUsage(BASE));
        START_RATES.keySet().forEach(code -> counters.add(new CurrencyUsage(code)));
        usageRepository.saveAll(counters);

        log.info("Seeded {} synthetic rate rows across {} days", batch.size(), days);
    }
}
