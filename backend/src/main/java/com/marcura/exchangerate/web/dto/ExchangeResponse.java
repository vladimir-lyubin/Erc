package com.marcura.exchangerate.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Response for {@code GET /exchange} (Appendix A). Every field is always present. */
public record ExchangeResponse(
        @NotNull String from,
        @NotNull String to,
        @NotNull BigDecimal exchange,
        @NotNull LocalDate date,
        long fromQueryCount,
        long toQueryCount
) {
}
