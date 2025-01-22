package com.neogroup.timelogs.cron;

import com.neogroup.timelogs.service.DatabaseHealthCheckerService;
import com.neogroup.timelogs.service.TimeLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimeLogCronJob {

    private static final Logger logger = LoggerFactory.getLogger(TimeLogCronJob.class);

    private final TimeLogService timeLogService;
    private final DatabaseHealthCheckerService databaseHealthCheckerService;

    @Autowired
    public TimeLogCronJob(TimeLogService timeLogService, DatabaseHealthCheckerService databaseHealthCheckerService) {
        this.timeLogService = timeLogService;
        this.databaseHealthCheckerService = databaseHealthCheckerService;
    }

    /**
     * Scheduled task to save the current time log every second
     */
    @Scheduled(fixedRate = 1000)
    public void logCurrentTime() {
        timeLogService.saveTimeLog(LocalDateTime.now());
    }

    /**
     * Scheduled task to check database availability every 5 seconds.
     * If DB is available, saved cached data from Redis.
     */
    @Scheduled(fixedRate = 5000)
    public void checkDatabaseAvailabilityAndFlush() {
        if (!timeLogService.getDatabaseAvailabilityStatus()) {
            try {
                boolean isAvailableNow = databaseHealthCheckerService.isDatabaseAvailable();
                timeLogService.setDatabaseAvailability(isAvailableNow);
                if (isAvailableNow) {
                    logger.info("Database is available now. Flushing cached logs from Redis");
                    timeLogService.flushCachedData();
                } else {
                    logger.info("Database is still unavailable.");
                }
            } catch (Exception e) {
                logger.error("Error checking DB availability: {}", e.getMessage());
            }
        }
    }
}
