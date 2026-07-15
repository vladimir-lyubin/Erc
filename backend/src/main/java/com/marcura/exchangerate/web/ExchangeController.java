package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.ExchangeRateService;
import com.marcura.exchangerate.service.TrendInsightService;
import com.marcura.exchangerate.web.dto.ExchangeResponse;
import com.marcura.exchangerate.web.dto.HistoricalRatesResponse;
import com.marcura.exchangerate.web.dto.InsightResponse;
import com.marcura.exchangerate.web.validation.CurrencyCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/exchange")
@Validated
@RequiredArgsConstructor
@Tag(name = "Exchange", description = "Spread-adjusted exchange rates and trend insight")
public class ExchangeController {

    private final ExchangeRateService exchangeRateService;
    private final TrendInsightService trendInsightService;

    @GetMapping
    @Operation(summary = "Spread-adjusted rate for a currency pair (latest or on a given date)")
    public ExchangeResponse exchange(
            @RequestParam @CurrencyCode String from,
            @RequestParam @CurrencyCode String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return exchangeRateService.getExchange(from, to, date);
    }

    @GetMapping("/historical")
    @Operation(summary = "Spread-adjusted rates over a date range (table + chart data)")
    public HistoricalRatesResponse historical(
            @RequestParam @CurrencyCode String from,
            @RequestParam @CurrencyCode String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return exchangeRateService.getHistorical(from, to, fromDate, toDate);
    }

    @GetMapping("/insight")
    @Operation(summary = "AI-generated trend insight for a currency pair over a period")
    public InsightResponse insight(
            @RequestParam @CurrencyCode String from,
            @RequestParam @CurrencyCode String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return trendInsightService.generateInsight(from, to, fromDate, toDate);
    }
}
