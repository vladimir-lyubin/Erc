package com.marcura.exchangerate.client;

import java.time.LocalDate;
import java.util.Map;

/**
 * Abstraction over the Fixer.io API so it can be mocked in tests and swapped for another provider.
 */
public interface FixerClient {

    /**
     * Latest rates keyed by currency code, relative to the base currency of the API key.
     */
    FixerRates fetchLatest();

    /**
     * @param base       base currency reported by the API (EUR on the free plan)
     * @param rateDate   the date the rates were calculated, as reported by the API
     * @param ratesByCode rate per currency code relative to {@code base}
     */
    record FixerRates(String base, LocalDate rateDate, Map<String, java.math.BigDecimal> ratesByCode) {
    }
}
