package com.marcura.exchangerate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration: Fixer.io integration and demo seeding.
 * Scheduler cron/zone are consumed directly via SpEL on {@code @Scheduled}, so they are not bound here.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Fixer fixer, Seed seed) {

    public record Fixer(String baseUrl, String apiKey) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Seed(boolean enabled, int days) {
    }
}
