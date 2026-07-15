package com.marcura.exchangerate.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Response for {@code GET /analytics} (suggested shape, Appendix A). */
public record AnalyticsResponse(List<CurrencyStat> topCurrencies) {

    public record CurrencyStat(String currency, long totalCount, LocalDate lastQueried) {
    }
}
