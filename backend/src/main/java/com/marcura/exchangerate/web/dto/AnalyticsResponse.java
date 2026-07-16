package com.marcura.exchangerate.web.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;

/** Response for {@code GET /analytics} (suggested shape, Appendix A). */
public record AnalyticsResponse(@NotNull List<CurrencyStat> topCurrencies) {

    /** {@code lastQueried} is {@code null} for a currency that has a counter row but was never queried. */
    public record CurrencyStat(@NotNull String currency, long totalCount, @Nullable LocalDate lastQueried) {
    }
}
