package com.marcura.exchangerate.web;

import com.marcura.exchangerate.client.FixerClient;
import com.marcura.exchangerate.domain.ExchangeRate;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import com.marcura.exchangerate.service.TrendInsightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the /exchange and /analytics endpoints against an in-memory H2 database.
 * The AI insight service is mocked so this test is isolated from any LLM.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExchangeApiIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2024, 3, 15);

    @Autowired
    private MockMvc mvc;

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
        rateRepository.save(new ExchangeRate("PLN", new BigDecimal("4.5"), DATE, "EUR"));
        rateRepository.save(new ExchangeRate("USD", new BigDecimal("1.08"), DATE, "EUR"));
    }

    @Test
    void returnsSpreadAdjustedRateAndIncrementsBothCounters() throws Exception {
        // EUR base spread 0%, PLN default 2.75% -> factor 0.9725 ; 4.5 * 0.9725 = 4.37625
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "PLN").param("date", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("EUR"))
                .andExpect(jsonPath("$.to").value("PLN"))
                .andExpect(jsonPath("$.date").value(DATE.toString()))
                .andExpect(jsonPath("$.exchange").value(comparesEqualTo(new BigDecimal("4.37625"))))
                .andExpect(jsonPath("$.fromQueryCount").value(1))
                .andExpect(jsonPath("$.toQueryCount").value(1));
    }

    @Test
    void incrementsCounterOnEachSuccessfulQuery() throws Exception {
        // Both calls involve EUR and USD, so both counters reach 2.
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "USD")).andExpect(status().isOk());
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromQueryCount").value(2))
                .andExpect(jsonPath("$.toQueryCount").value(2));

        assertThat(usageRepository.findById("EUR")).get()
                .extracting(u -> u.getQueryCount()).isEqualTo(2L);
    }

    @Test
    void usesLatestDateWhenDateOmitted() throws Exception {
        // No date param -> service falls back to the most recent stored rate date (DATE).
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "PLN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(DATE.toString()));
    }

    @Test
    void acceptsLowercaseCurrencyAndNormalisesResponse() throws Exception {
        mvc.perform(get("/exchange").param("from", "eur").param("to", "pln").param("date", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("EUR"))
                .andExpect(jsonPath("$.to").value("PLN"))
                .andExpect(jsonPath("$.exchange").value(comparesEqualTo(new BigDecimal("4.37625"))));
    }

    @Test
    void sameCurrencyPairAppliesSpreadAndCountsThatCurrency() throws Exception {
        // USD->USD: cross-rate 1, USD default spread 2.75% -> 0.9725; the USD counter is bumped for both legs.
        mvc.perform(get("/exchange").param("from", "USD").param("to", "USD").param("date", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchange").value(comparesEqualTo(new BigDecimal("0.9725"))));

        assertThat(usageRepository.findById("USD")).get()
                .extracting(u -> u.getQueryCount()).isEqualTo(2L);
    }

    @Test
    void returns404WhenRateForDateMissing() throws Exception {
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "PLN").param("date", "2000-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400ForUnknownCurrency() throws Exception {
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "ZZZ"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenRequiredParamMissing() throws Exception {
        // 'to' is required — the advice turns the MissingServletRequestParameterException into 400.
        mvc.perform(get("/exchange").param("from", "EUR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400ForMalformedDate() throws Exception {
        // Bad date format -> type mismatch, mapped to 400 by the centralized handler.
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "USD").param("date", "15-03-2024"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyticsReflectsUsage() throws Exception {
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "PLN")).andExpect(status().isOk());
        mvc.perform(get("/exchange").param("from", "EUR").param("to", "USD")).andExpect(status().isOk());

        mvc.perform(get("/analytics"))
                .andExpect(status().isOk())
                // EUR involved in both queries -> highest count, listed first
                .andExpect(jsonPath("$.topCurrencies[0].currency").value("EUR"))
                .andExpect(jsonPath("$.topCurrencies[0].totalCount").value(2));
    }
}
