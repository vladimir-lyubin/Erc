package com.marcura.exchangerate.web.validation;

import com.marcura.exchangerate.util.CurrencyUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that a value is a known ISO 4217 currency code. Stateless and reusable.
 */
public class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {

    private static final Set<String> ISO_CODES = Currency.getAvailableCurrencies().stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null-ness is handled by @NotNull where required
        }
        return ISO_CODES.contains(CurrencyUtils.normalize(value));
    }
}
