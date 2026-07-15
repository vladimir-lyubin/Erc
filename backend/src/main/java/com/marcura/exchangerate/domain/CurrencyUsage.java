package com.marcura.exchangerate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Usage counter per currency. Incremented atomically via a modifying query
 * (see {@code CurrencyUsageRepository}) to stay correct under concurrent requests.
 */
@Entity
@Table(name = "currency_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurrencyUsage {

    @Id
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "query_count", nullable = false)
    private long queryCount;

    @Column(name = "last_queried_date")
    private LocalDate lastQueriedDate;

    public CurrencyUsage(String currencyCode) {
        this.currencyCode = currencyCode;
        this.queryCount = 0;
    }
}
