package com.marcura.exchangerate.client;

import com.marcura.exchangerate.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

/**
 * {@link RestClient}-based Fixer.io implementation. On the free plan the base currency is fixed to EUR
 * and there is no historical endpoint, so only {@code /latest} is used.
 * <p>
 * All resilience concerns live here in the REST client: explicit connect/read timeouts so a slow
 * upstream fails fast, and a declarative retry on {@link #fetchLatest()} — transient network errors and
 * 5xx responses are retried with exponential backoff, then {@link #recover(RestClientException)} turns
 * exhausted retries into a clean 503 instead of leaking a timeout stack trace.
 */
@Component
@Slf4j
public class FixerRestClient implements FixerClient {

    private final RestClient restClient;
    private final AppProperties.Fixer config;

    public FixerRestClient(RestClient.Builder builder, AppProperties properties) {
        this.config = properties.fixer();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.connectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(config.readTimeoutMs()));
        this.restClient = builder
                .baseUrl(config.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttemptsExpression = "${app.fixer.max-attempts}",
            backoff = @Backoff(delayExpression = "${app.fixer.retry-backoff-ms}",
                    multiplierExpression = "${app.fixer.retry-multiplier}"))
    public FixerRates fetchLatest() {
        if (!config.hasApiKey()) {
            throw new IllegalStateException("FIXER_API_KEY is not configured");
        }
        LatestResponse response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/latest").queryParam("access_key", config.apiKey());
                    if (config.hasSymbols()) {
                        uriBuilder.queryParam("symbols", String.join(",", config.symbols()));
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(LatestResponse.class);

        if (response == null || !response.success()) {
            throw new IllegalStateException("Fixer.io returned an unsuccessful response");
        }
        return new FixerRates(response.base(), LocalDate.parse(response.date()), response.rates());
    }

    /**
     * Fallback invoked once all retries are exhausted. We have no cached source of truth to fall back
     * to, so the minimal safe behaviour is to surface the outage as an {@link IllegalStateException}
     * (mapped to HTTP 503 by the global handler; scheduled runs simply log and skip this cycle).
     */
    @Recover
    FixerRates recover(RestClientException ex) {
        log.error("Fixer.io unreachable after {} attempts: {}", config.maxAttempts(), ex.getMessage());
        throw new IllegalStateException("Fixer.io is temporarily unavailable (failed after retries)");
    }

    /** Minimal projection of the Fixer.io /latest payload we actually consume. */
    private record LatestResponse(boolean success, String base, String date, Map<String, BigDecimal> rates) {
    }
}
