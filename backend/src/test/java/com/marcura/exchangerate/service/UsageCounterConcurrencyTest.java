package com.marcura.exchangerate.service;

import com.marcura.exchangerate.domain.CurrencyUsage;
import com.marcura.exchangerate.domain.ExchangeRate;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the usage counter stays correct under concurrent /exchange queries. Each query increments
 * both currencies via a single atomic UPDATE, so N parallel calls must yield exactly N.
 */
@SpringBootTest
@ActiveProfiles("test")
class UsageCounterConcurrencyTest {

    private static final LocalDate DATE = LocalDate.of(2024, 3, 15);
    private static final int THREADS = 50;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ExchangeRateRepository rateRepository;

    @Autowired
    private CurrencyUsageRepository usageRepository;

    @MockBean
    private TrendInsightService trendInsightService;

    @BeforeEach
    void setUp() {
        usageRepository.deleteAll();
        rateRepository.deleteAll();
        rateRepository.save(new ExchangeRate("EUR", BigDecimal.ONE, DATE, "EUR"));
        rateRepository.save(new ExchangeRate("USD", new BigDecimal("1.08"), DATE, "EUR"));
        // Counter rows exist up-front, so every concurrent query is a pure atomic UPDATE.
        usageRepository.save(new CurrencyUsage("EUR"));
        usageRepository.save(new CurrencyUsage("USD"));
    }

    @Test
    void concurrentQueriesProduceExactCount() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(pool.submit(() -> exchangeRateService.getExchange("EUR", "USD", DATE)));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(usageRepository.findById("EUR")).get()
                .extracting(u -> u.getQueryCount()).isEqualTo((long) THREADS);
        assertThat(usageRepository.findById("USD")).get()
                .extracting(u -> u.getQueryCount()).isEqualTo((long) THREADS);
    }
}
