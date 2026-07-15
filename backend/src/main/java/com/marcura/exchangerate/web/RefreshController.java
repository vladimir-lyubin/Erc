package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.RateCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Optional extension: manually trigger a fetch + upsert without touching usage counters. */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Operational endpoints")
public class RefreshController {

    private final RateCollectionService rateCollectionService;

    @PostMapping("/refresh")
    @Operation(summary = "Manually trigger a rate fetch and upsert (does not affect usage counters)")
    public Map<String, Integer> refresh() {
        return Map.of("upserted", rateCollectionService.collectAndUpsert());
    }
}
