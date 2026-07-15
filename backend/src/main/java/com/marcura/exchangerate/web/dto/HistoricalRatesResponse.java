package com.marcura.exchangerate.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Raw spread-adjusted rates over a period, feeding the frontend table + chart. */
public record HistoricalRatesResponse(
        String from,
        String to,
        LocalDate fromDate,
        LocalDate toDate,
        List<Point> points
) {
    public record Point(LocalDate date, BigDecimal rate) {
    }
}
