package com.marcura.exchangerate.service.impl;

import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.service.AnalyticsService;
import com.marcura.exchangerate.web.dto.AnalyticsResponse;
import com.marcura.exchangerate.web.dto.AnalyticsResponse.CurrencyStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CurrencyUsageRepository usageRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getUsageAnalytics() {
        var stats = usageRepository.findAllByOrderByQueryCountDesc().stream()
                .map(u -> new CurrencyStat(u.getCurrencyCode(), u.getQueryCount(), u.getLastQueriedDate()))
                .toList();
        return new AnalyticsResponse(stats);
    }
}
