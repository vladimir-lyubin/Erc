package com.marcura.exchangerate.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Response for {@code GET /exchange/insight} (suggested shape, Appendix A). */
public record InsightResponse(
        @NotNull String from,
        @NotNull String to,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull String insight
) {
}
