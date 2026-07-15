package com.marcura.exchangerate.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom Jakarta Bean Validation constraint for a 3-letter ISO-4217 currency code.
 * Backed by {@link CurrencyCodeValidator}. Use instead of ad-hoc {@code @Pattern} so the rule
 * lives in one reusable place.
 */
@Documented
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface CurrencyCode {

    String message() default "must be a valid 3-letter ISO 4217 currency code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
