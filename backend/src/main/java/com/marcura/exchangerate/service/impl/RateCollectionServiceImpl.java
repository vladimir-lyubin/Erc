package com.marcura.exchangerate.service.impl;

import com.marcura.exchangerate.client.FixerClient;
import com.marcura.exchangerate.client.FixerClient.FixerRates;
import com.marcura.exchangerate.domain.CurrencyUsage;
import com.marcura.exchangerate.domain.ExchangeRate;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import com.marcura.exchangerate.service.RateCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateCollectionServiceImpl implements RateCollectionService {

    private final FixerClient fixerClient;
    private final ExchangeRateRepository rateRepository;
    private final CurrencyUsageRepository usageRepository;

    @Override
    @Transactional
    public int collectAndUpsert() {
        FixerRates rates = fixerClient.fetchLatest();
        int upserted = 0;

        // The base currency itself is stored with rate 1 so base->X queries resolve.
        upserted += upsert(rates.base(), BigDecimal.ONE, rates.rateDate(), rates.base());

        for (Map.Entry<String, BigDecimal> entry : rates.ratesByCode().entrySet()) {
            upserted += upsert(entry.getKey(), entry.getValue(), rates.rateDate(), rates.base());
        }
        log.info("Upserted {} rates for {}", upserted, rates.rateDate());
        return upserted;
    }

    /** Ensures a usage counter row exists so /exchange increments never need to insert under load. */
    private void ensureUsageRow(String currency) {
        if (!usageRepository.existsById(currency)) {
            usageRepository.save(new CurrencyUsage(currency));
        }
    }

    /**
     * Idempotent upsert keyed on (currency, date). Safe to re-run for the same day and tolerant of
     * a concurrent insert from another instance thanks to the unique constraint.
     */
    int upsert(String currency, BigDecimal rate, LocalDate rateDate, String base) {
        ensureUsageRow(currency);
        return rateRepository.findByCurrencyCodeAndRateDate(currency, rateDate)
                .map(existing -> {
                    existing.setRate(rate);
                    rateRepository.save(existing);
                    return 1;
                })
                .orElseGet(() -> {
                    try {
                        rateRepository.save(new ExchangeRate(currency, rate, rateDate, base));
                        return 1;
                    } catch (DataIntegrityViolationException concurrent) {
                        log.debug("Rate for {}/{} inserted concurrently, skipping", currency, rateDate);
                        return 0;
                    }
                });
    }
}
