package com.marcura.exchangerate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Application configuration: Fixer.io integration and demo seeding.
 * Scheduler cron/zone are consumed directly via SpEL on {@code @Scheduled}, so they are not bound here.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Fixer fixer, Seed seed) {

    /**
     * @param symbols non-base currencies to request from Fixer (EUR is the free-plan base and is
     *                always stored implicitly with rate 1). Empty means "all currencies Fixer returns".
     */
    public record Fixer(String baseUrl, String apiKey, List<String> symbols) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean hasSymbols() {
            return symbols != null && !symbols.isEmpty();
        }
    }

    public record Seed(boolean enabled, int days) {
    }
}
