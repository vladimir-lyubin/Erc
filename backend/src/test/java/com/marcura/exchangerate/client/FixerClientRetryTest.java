package com.marcura.exchangerate.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the declarative retry + {@code @Recover} fallback around the Fixer.io client. The base URL
 * points at a refused port so every attempt fails with a transient {@link org.springframework.web.client.ResourceAccessException},
 * exhausts the retries, and is turned into a clean {@link IllegalStateException} (→ HTTP 503) by the recover method.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.fixer.api-key=dummy-key",
        "app.fixer.base-url=http://localhost:1",
        "app.fixer.connect-timeout-ms=200",
        "app.fixer.read-timeout-ms=200",
        "app.fixer.max-attempts=2",
        "app.fixer.retry-backoff-ms=10"
})
class FixerClientRetryTest {

    @Autowired
    private FixerClient fixerClient;

    @Test
    void exhaustedRetriesFallBackToServiceUnavailable() {
        assertThatThrownBy(fixerClient::fetchLatest)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("temporarily unavailable");
    }
}
