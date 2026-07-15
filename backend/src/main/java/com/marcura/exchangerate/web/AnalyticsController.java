package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.AnalyticsService;
import com.marcura.exchangerate.web.dto.AnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Currency usage statistics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "Usage statistics per currency (counts and last-queried dates)")
    public AnalyticsResponse analytics() {
        return analyticsService.getUsageAnalytics();
    }
}
