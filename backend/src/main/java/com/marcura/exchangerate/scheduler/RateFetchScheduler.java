package com.marcura.exchangerate.scheduler;

import com.marcura.exchangerate.service.RateCollectionService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily collection at 12:05 AM GMT.
 * <p>
 * {@link SchedulerLock} (ShedLock) ensures only one instance runs the job in a multi-instance
 * deployment. Rationale over leader election: no extra infrastructure, uses the existing datasource,
 * and the job is naturally idempotent thanks to the upsert. See PLAN.md §5.
 */
@Component
public class RateFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(RateFetchScheduler.class);

    private final RateCollectionService rateCollectionService;

    public RateFetchScheduler(RateCollectionService rateCollectionService) {
        this.rateCollectionService = rateCollectionService;
    }

    @Scheduled(cron = "${app.scheduler.cron}", zone = "${app.scheduler.zone}")
    @SchedulerLock(name = "dailyRateFetch", lockAtMostFor = "PT9M", lockAtLeastFor = "PT1M")
    public void fetchDailyRates() {
        log.info("Starting scheduled daily rate collection");
        int upserted = rateCollectionService.collectAndUpsert();
        log.info("Daily rate collection finished, {} rates upserted", upserted);
    }
}
