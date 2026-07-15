package com.marcura.exchangerate.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Response for {@code GET /exchange} (Appendix A). */
public record ExchangeResponse(
        String from,
        String to,
        BigDecimal exchange,
        LocalDate date,
        long fromQueryCount,
        long toQueryCount
) {
}
