package com.marcura.exchangerate.service;

import com.marcura.exchangerate.web.dto.ExchangeResponse;
import com.marcura.exchangerate.web.dto.HistoricalRatesResponse;

import java.time.LocalDate;

/**
 * Computes spread-adjusted rates from locally stored data and records usage.
 *
 * <p>Implementation notes (see PLAN.md):
 * <ul>
 *   <li>When {@code date} is null, use the most recent available rate date.</li>
 *   <li>Throw {@link com.marcura.exchangerate.exception.RateNotFoundException} when a rate is missing (→ 404).</li>
 *   <li>Increment usage counters for BOTH currencies atomically on each successful query.</li>
 * </ul>
 */
public interface ExchangeRateService {

    ExchangeResponse getExchange(String from, String to, LocalDate date);

    HistoricalRatesResponse getHistorical(String from, String to, LocalDate fromDate, LocalDate toDate);
}
