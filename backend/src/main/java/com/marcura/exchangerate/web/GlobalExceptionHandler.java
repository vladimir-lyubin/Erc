package com.marcura.exchangerate.web;

import com.marcura.exchangerate.exception.RateNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Single place where every exception thrown by a controller is turned into a consistent RFC-7807
 * {@link ProblemDetail} JSON body. Standard Spring MVC errors (missing/!typed params, unknown path,
 * unsupported method, unreadable body, ...) are handled by the {@link ResponseEntityExceptionHandler}
 * base class; the handlers below add the domain-specific and catch-all cases.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RateNotFoundException.class)
    public ProblemDetail handleNotFound(RateNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Rate not found", ex.getMessage());
    }

    /** Bean Validation failures on {@code @RequestParam}/path params (e.g. an invalid currency code). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::formatViolation)
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed",
                detail.isBlank() ? ex.getMessage() : detail);
    }

    /**
     * Upstream/config problems surfaced synchronously (e.g. missing Fixer API key or an unsuccessful
     * Fixer response during a manual /admin/refresh). Reported as 503 so the caller can retry.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleUpstreamUnavailable(IllegalStateException ex) {
        log.warn("Upstream/service unavailable: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable", ex.getMessage());
    }

    /** Catch-all: never leak stack traces to the client, but log them server-side. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred. Please try again later.");
    }

    private static String formatViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return "%s: %s".formatted(field, violation.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
