package com.marcura.exchangerate.exception;

/** Thrown when no rate exists for a requested currency/date. Mapped to HTTP 404. */
public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(String message) {
        super(message);
    }
}
