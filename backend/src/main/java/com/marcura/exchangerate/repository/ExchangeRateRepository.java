package com.marcura.exchangerate.repository;

import com.marcura.exchangerate.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyCodeAndRateDate(String currencyCode, LocalDate rateDate);

    /** Most recent date for which we hold any rate (used when no date is supplied). */
    @Query("select max(r.rateDate) from ExchangeRate r")
    Optional<LocalDate> findLatestRateDate();

    List<ExchangeRate> findByCurrencyCodeAndRateDateBetweenOrderByRateDateAsc(
            String currencyCode, LocalDate from, LocalDate to);

    @Query("""
            select r from ExchangeRate r
            where r.currencyCode = :currency and r.rateDate = :date
            """)
    Optional<ExchangeRate> findRate(@Param("currency") String currency, @Param("date") LocalDate date);
}
