package com.marcura.exchangerate.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Raw spread-adjusted rates over a period, feeding the frontend table + chart. */
public record HistoricalRatesResponse(
        @NotNull String from,
        @NotNull String to,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull List<Point> points
) {
    public record Point(@NotNull LocalDate date, @NotNull BigDecimal rate) {
    }
}
