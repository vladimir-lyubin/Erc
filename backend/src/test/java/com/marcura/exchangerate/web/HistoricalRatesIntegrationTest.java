package com.marcura.exchangerate.web;

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
 * Edge cases for {@code GET /exchange/historical}: a point is produced only for dates where BOTH
 * currencies have a stored rate, results are date-ascending, empty/no-overlap ranges yield an empty
 * list (not a 404), and browsing history never touches the usage counters.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoricalRatesIntegrationTest {

    private static final LocalDate D1 = LocalDate.of(2024, 3, 1);
    private static final LocalDate D2 = LocalDate.of(2024, 3, 2);
    private static final LocalDate D3 = LocalDate.of(2024, 3, 3);

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
        // EUR (base) and USD present on all three days; PLN has a gap on D2.
        for (LocalDate d : new LocalDate[] {D1, D2, D3}) {
            rateRepository.save(new ExchangeRate("EUR", BigDecimal.ONE, d, "EUR"));
        }
        rateRepository.save(new ExchangeRate("USD", new BigDecimal("1.00"), D1, "EUR"));
        rateRepository.save(new ExchangeRate("USD", new BigDecimal("1.10"), D2, "EUR"));
        rateRepository.save(new ExchangeRate("USD", new BigDecimal("1.20"), D3, "EUR"));
        rateRepository.save(new ExchangeRate("PLN", new BigDecimal("4.50"), D1, "EUR"));
        rateRepository.save(new ExchangeRate("PLN", new BigDecimal("4.60"), D3, "EUR"));
    }

    @Test
    void returnsOneAscendingPointPerOverlappingDate() throws Exception {
        // EUR (0%) / USD (2.75%) -> factor 0.9725. D1 rate 1.00 -> 0.9725.
        mvc.perform(historical("EUR", "USD", D1, D3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.points[0].date").value(D1.toString()))
                .andExpect(jsonPath("$.points[0].rate").value(comparesEqualTo(new BigDecimal("0.9725"))))
                .andExpect(jsonPath("$.points[2].date").value(D3.toString()));
    }

    @Test
    void skipsDatesWhereOneCurrencyIsMissing() throws Exception {
        // PLN is absent on D2 -> only D1 and D3 produce points.
        mvc.perform(historical("EUR", "PLN", D1, D3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(2))
                .andExpect(jsonPath("$.points[0].date").value(D1.toString()))
                .andExpect(jsonPath("$.points[1].date").value(D3.toString()));
    }

    @Test
    void singleDayRangeReturnsSinglePoint() throws Exception {
        mvc.perform(historical("EUR", "USD", D2, D2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(1))
                .andExpect(jsonPath("$.points[0].date").value(D2.toString()));
    }

    @Test
    void rangeWithoutDataReturnsEmptyListNot404() throws Exception {
        mvc.perform(historical("EUR", "USD", LocalDate.of(1999, 1, 1), LocalDate.of(1999, 1, 31)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    void invertedRangeReturnsEmptyList() throws Exception {
        // fromDate after toDate -> the BETWEEN query matches nothing; empty list, still 200.
        mvc.perform(historical("EUR", "USD", D3, D1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    void historicalBrowsingDoesNotTouchUsageCounters() throws Exception {
        mvc.perform(historical("EUR", "USD", D1, D3)).andExpect(status().isOk());
        assertThat(usageRepository.count()).isZero();
    }

    private static org.springframework.test.web.servlet.RequestBuilder historical(
            String from, String to, LocalDate fromDate, LocalDate toDate) {
        return get("/exchange/historical")
                .param("from", from)
                .param("to", to)
                .param("fromDate", fromDate.toString())
                .param("toDate", toDate.toString());
    }
}
