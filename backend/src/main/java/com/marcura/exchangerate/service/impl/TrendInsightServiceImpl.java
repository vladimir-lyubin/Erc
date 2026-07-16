package com.marcura.exchangerate.service.impl;

import com.marcura.exchangerate.service.ExchangeRateService;
import com.marcura.exchangerate.service.TrendInsightService;
import com.marcura.exchangerate.util.CurrencyUtils;
import com.marcura.exchangerate.web.dto.HistoricalRatesResponse;
import com.marcura.exchangerate.web.dto.InsightResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Produces a short natural-language trend insight via Spring AI. The actual rate points for the
 * requested period are injected into the user prompt, so the model reasons over real numbers.
 * The system prompt constrains the answer to a concise, relevant comment.
 */
@Service
@Slf4j
public class TrendInsightServiceImpl implements TrendInsightService {

    private static final String SYSTEM_PROMPT = """
            You are a financial assistant. You are given a time series of daily exchange rates for a
            currency pair. Write ONE or TWO concise sentences describing the trend a user would notice:
            overall direction, approximate percentage change, and any notable move. Only use the numbers
            provided. Do not invent data, give advice, or add disclaimers. Plain text, no markdown.
            """;

    private final ChatClient chatClient;
    private final ExchangeRateService exchangeRateService;

    public TrendInsightServiceImpl(ChatClient.Builder chatClientBuilder,
                                   ExchangeRateService exchangeRateService) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    public InsightResponse generateInsight(String from, String to, LocalDate fromDate, LocalDate toDate) {
        HistoricalRatesResponse history = exchangeRateService.getHistorical(from, to, fromDate, toDate);
        String insight = history.points().isEmpty()
                ? "No rate data is available for %s/%s in the selected period.".formatted(from, to)
                : askModel(from, to, history);
        return new InsightResponse(CurrencyUtils.normalize(from), CurrencyUtils.normalize(to),
                fromDate, toDate, insight);
    }

    private String askModel(String from, String to, HistoricalRatesResponse history) {
        String series = history.points().stream()
                .map(p -> "%s=%s".formatted(p.date(), p.rate().toPlainString()))
                .collect(Collectors.joining(", "));
        String userPrompt = "Currency pair %s/%s. Daily spread-adjusted rates: %s".formatted(from, to, series);
        try {
            return chatClient.prompt().user(userPrompt).call().content();
        } catch (RuntimeException ex) {
            log.warn("LLM insight generation failed, returning fallback", ex);
            return "Trend insight is temporarily unavailable (the model could not be reached).";
        }
    }
}
