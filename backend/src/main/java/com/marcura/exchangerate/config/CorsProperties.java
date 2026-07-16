package com.marcura.exchangerate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS settings bound from the {@code cors.*} block in {@code application.yml}, so origins and methods
 * are configurable per environment without touching code.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins, List<String> allowedMethods) {
}
