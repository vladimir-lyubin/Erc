package com.marcura.exchangerate.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

/**
 * Turns Bean Validation failures into a compact, human-readable message for error responses,
 * keeping the formatting rules (and their literals) out of the exception handler.
 */
@NoArgsConstructor
public final class ValidationMessageUtils {

    private static final String FIELD_MESSAGE_FORMAT = "%s: %s";
    private static final String VIOLATION_DELIMITER = "; ";

    /**
     * Semicolon-separated summary of every violation, each rendered as {@code "field: message"}
     * (e.g. {@code "from: must be a valid ISO 4217 currency code"}).
     */
    public static String describe(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(ValidationMessageUtils::formatViolation)
                .collect(Collectors.joining(VIOLATION_DELIMITER));
    }

    private static String formatViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return String.format(FIELD_MESSAGE_FORMAT, field, violation.getMessage());
    }
}
