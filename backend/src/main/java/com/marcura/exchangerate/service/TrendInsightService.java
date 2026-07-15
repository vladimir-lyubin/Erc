package com.marcura.exchangerate.service;

import com.marcura.exchangerate.web.dto.InsightResponse;

import java.time.LocalDate;

/**
 * Generates a short natural-language insight about a rate trend by calling an LLM through Spring AI.
 * The historical rate points for the period MUST be injected into the prompt as context so the model
 * reasons over the real numbers rather than producing generic filler.
 */
public interface TrendInsightService {

    InsightResponse generateInsight(String from, String to, LocalDate fromDate, LocalDate toDate);
}
