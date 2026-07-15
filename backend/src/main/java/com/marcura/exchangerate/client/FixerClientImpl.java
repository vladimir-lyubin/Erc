package com.marcura.exchangerate.client;

import com.marcura.exchangerate.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Fixer.io implementation. On the free plan the base currency is fixed to EUR and there is no
 * historical endpoint, so only {@code /latest} is used.
 */
@Component
@Slf4j
public class FixerClientImpl implements FixerClient {

    private final RestClient restClient;
    private final AppProperties.Fixer config;

    public FixerClientImpl(RestClient.Builder builder, AppProperties properties) {
        this.config = properties.fixer();
        this.restClient = builder.baseUrl(config.baseUrl()).build();
    }

    @Override
    public FixerRates fetchLatest() {
        if (!config.hasApiKey()) {
            throw new IllegalStateException("FIXER_API_KEY is not configured");
        }
        LatestResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/latest").queryParam("access_key", config.apiKey()).build())
                .retrieve()
                .body(LatestResponse.class);

        if (response == null || !response.success()) {
            throw new IllegalStateException("Fixer.io returned an unsuccessful response");
        }
        return new FixerRates(response.base(), LocalDate.parse(response.date()), response.rates());
    }

    /** Minimal projection of the Fixer.io /latest payload we actually consume. */
    private record LatestResponse(boolean success, String base, String date, Map<String, BigDecimal> rates) {
    }
}
