package com.marcura.exchangerate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single currency rate as reported by Fixer.io for a given date.
 * <p>
 * {@code rateDate} is the date reported by the API (not the fetch date).
 * The unique constraint on {@code (currencyCode, rateDate)} is the basis for idempotent upserts.
 */
@Entity
@Table(
        name = "exchange_rate",
        uniqueConstraints = @UniqueConstraint(name = "uk_rate_currency_date", columnNames = {"currency_code", "rate_date"}),
        indexes = @Index(name = "idx_rate_currency_date", columnList = "currency_code, rate_date")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /** Rate relative to the base currency returned by the Fixer.io key (EUR on the free plan). */
    @Setter
    @Column(name = "rate", nullable = false, precision = 24, scale = 12)
    private BigDecimal rate;

    /** Date the rate was calculated, as reported by the API. */
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ExchangeRate(String currencyCode, BigDecimal rate, LocalDate rateDate, String baseCurrency) {
        this.currencyCode = currencyCode;
        this.rate = rate;
        this.rateDate = rateDate;
        this.baseCurrency = baseCurrency;
    }
}
