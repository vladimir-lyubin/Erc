package com.marcura.exchangerate.util;

import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Stateless helpers for currency codes, shared by services, the spread calculator and the validator
 * so the normalization rule lives in exactly one place.
 */
@NoArgsConstructor
public final class CurrencyUtils {

    /**
     * Canonical form of a currency code: trimmed and upper-cased with {@link Locale#ROOT}
     * (avoids locale-specific casing bugs such as the Turkish dotless-i). Null-safe.
     *
     * @return the normalized code, or {@code null} if the input was {@code null}
     */
    public static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
