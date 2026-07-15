package com.marcura.exchangerate.repository;

import com.marcura.exchangerate.domain.CurrencyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CurrencyUsageRepository extends JpaRepository<CurrencyUsage, String> {

    /**
     * Atomic increment. Returns the number of rows affected (0 when the row does not exist yet,
     * in which case the caller inserts a fresh counter and retries).
     * Keeping the increment in the database avoids read-modify-write races across threads/instances.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CurrencyUsage u
            set u.queryCount = u.queryCount + 1,
                u.lastQueriedDate = :date
            where u.currencyCode = :currency
            """)
    int incrementUsage(@Param("currency") String currency, @Param("date") LocalDate date);

    List<CurrencyUsage> findAllByOrderByQueryCountDesc();
}
