package com.marcura.exchangerate.service;

/**
 * Fetches rates from Fixer.io and upserts them into the database.
 * Upsert keys on {@code (currencyCode, rateDate)} where {@code rateDate} comes from the API response,
 * making a re-run for the same day idempotent (no duplicate rows, no counter side effects).
 */
public interface RateCollectionService {

    /** Fetch latest rates and upsert. Used by the scheduler and the optional manual-refresh endpoint. */
    int collectAndUpsert();
}
