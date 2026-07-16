package com.marcura.exchangerate.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyUtilsTest {

    @Test
    void normalizeUppercasesAndTrims() {
        assertThat(CurrencyUtils.normalize("  usd ")).isEqualTo("USD");
        assertThat(CurrencyUtils.normalize("Eur")).isEqualTo("EUR");
    }

    @Test
    void normalizeReturnsNullForNull() {
        assertThat(CurrencyUtils.normalize(null)).isNull();
    }

    @Test
    void normalizeUsesLocaleRootRegardlessOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            // Turkish locale would upper-case "i" to a dotted "İ"; ROOT must avoid that.
            Locale.setDefault(new Locale("tr", "TR"));
            assertThat(CurrencyUtils.normalize("idr")).isEqualTo("IDR");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void normalizeLeavesAlreadyCanonicalCodeUnchanged() {
        assertThat(CurrencyUtils.normalize("GBP")).isEqualTo("GBP");
    }
}
