package com.marcura.exchangerate.web.dto;

import java.time.LocalDate;

/** Response for {@code GET /exchange/insight} (suggested shape, Appendix A). */
public record InsightResponse(
        String from,
        String to,
        LocalDate fromDate,
        LocalDate toDate,
        String insight
) {
}
