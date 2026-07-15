package com.marcura.exchangerate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Spread reference table (Appendix B), externalised so business logic never hardcodes rates.
 * {@code groups} maps a spread percent (as string key) to the list of currency codes in that group.
 * The base currency carries a 0% spread and is resolved separately.
 */
@ConfigurationProperties(prefix = "app.spread")
public record SpreadProperties(
        String defaultPercent,
        Map<String, List<String>> groups
) {
}
