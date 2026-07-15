package com.marcura.exchangerate.service;

import com.marcura.exchangerate.client.FixerClient;
import com.marcura.exchangerate.client.FixerClient.FixerRates;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The daily collection upsert must be idempotent: re-running for the same API-reported date updates
 * existing rows instead of duplicating them (so a multi-instance or retried run is safe).
 */
@SpringBootTest
@ActiveProfiles("test")
class RateCollectionServiceTest {

    private static final LocalDate DATE = LocalDate.of(2024, 3, 15);

    @Autowired
    private RateCollectionService rateCollectionService;

    @Autowired
    private ExchangeRateRepository rateRepository;

    @MockBean
    private TrendInsightService trendInsightService;

    @MockBean
    private FixerClient fixerClient;

    @BeforeEach
    void setUp() {
        rateRepository.deleteAll();
    }

    @Test
    void reRunForSameDateUpdatesInsteadOfDuplicating() {
        BDDMockito.given(fixerClient.fetchLatest()).willReturn(
                new FixerRates("EUR", DATE, Map.of("USD", new BigDecimal("1.08"), "PLN", new BigDecimal("4.56"))));

        rateCollectionService.collectAndUpsert();
        long afterFirst = rateRepository.count(); // EUR (base) + USD + PLN = 3

        rateCollectionService.collectAndUpsert();
        long afterSecond = rateRepository.count();

        assertThat(afterFirst).isEqualTo(3);
        assertThat(afterSecond).isEqualTo(3);
    }

    @Test
    void reRunWithChangedRateUpdatesStoredValue() {
        BDDMockito.given(fixerClient.fetchLatest()).willReturn(
                new FixerRates("EUR", DATE, Map.of("USD", new BigDecimal("1.08"))));
        rateCollectionService.collectAndUpsert();

        BDDMockito.given(fixerClient.fetchLatest()).willReturn(
                new FixerRates("EUR", DATE, Map.of("USD", new BigDecimal("1.10"))));
        rateCollectionService.collectAndUpsert();

        BigDecimal stored = rateRepository.findByCurrencyCodeAndRateDate("USD", DATE)
                .orElseThrow().getRate();
        assertThat(stored).isEqualByComparingTo("1.10");
    }
}
