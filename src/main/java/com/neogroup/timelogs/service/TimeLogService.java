package com.neogroup.timelogs.service;

import com.neogroup.timelogs.dto.TimeLogDto;
import com.neogroup.timelogs.entity.TimeLog;
import com.neogroup.timelogs.mapper.TimeLogMapper;
import com.neogroup.timelogs.repository.TimeLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TimeLogService {

    private static final Logger logger = LoggerFactory.getLogger(TimeLogService.class);
    private static final String FAILED_TIME_LOG_KEY = "failedTimeLogs";

    private final TimeLogRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    private boolean isDatabaseAvailable = true;
    private boolean isFlushing = false;

    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    public TimeLogService(TimeLogRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Save method for write current time.
     * If DB is unavailable, data is saved to Redis.
     */
    public void saveTimeLog(LocalDateTime currentTime) {
        if (isFlushing) {
            logger.info("Skipping saveTimeLog because flushCachedData is running.");
            return;
        }

        if (currentTime == null) {
            currentTime = LocalDateTime.now();
        }
        if (isDatabaseAvailable) {
            try {
                flushCachedData();
                repository.save(new TimeLog(currentTime));
                logger.info("Successfully saved time log to the database: {}", currentTime);
            } catch (Exception e) {
                logger.warn("Failed to save time log to DB. Saving to Redis: {}", currentTime);
                cacheUnsavedTimeLogs(currentTime);
                isDatabaseAvailable = false;
            }
        } else {
            // Save the time log to Redis if the DB is unavailable
            cacheUnsavedTimeLogs(currentTime);
            logger.info("Database is unavailable. Saved time log to Redis: {}", currentTime);
        }
    }

    /**
     * Saves the current timestamp to Redis when DB is unavailable.
     * Redis Sorted Set uses the timestamp as the score for ordering.
     */
    public void cacheUnsavedTimeLogs(LocalDateTime currentTime) {
        if (currentTime == null) {
            logger.warn("No timestamp available for recovery. Skipping...");
            return;
        }
        redisTemplate.opsForZSet().add(FAILED_TIME_LOG_KEY, currentTime, currentTime.toEpochSecond(java.time.ZoneOffset.UTC));
        logger.warn("Saved tme log to Redis for retry: {}", currentTime);
    }

    /**
     * Retries writing failed timestamps from the Redis to the database.
     */
    public void flushCachedData() {
        if (!lock.tryLock()) {
            logger.info("flushCachedData is already running.");
            return;
        }
        isFlushing = true;
        try {
            logger.info("Starting to flush cached data from Redis.");
            Set<Object> failedTimestamps = redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1);
            if (failedTimestamps != null && !failedTimestamps.isEmpty()) {
                for (Object timestamp : failedTimestamps) {
                    try {
                        LocalDateTime timeStampToSave = LocalDateTime.parse((String) timestamp);
                        repository.save(new TimeLog(timeStampToSave));
                        redisTemplate.opsForZSet().remove(FAILED_TIME_LOG_KEY, timeStampToSave);
                        logger.info("Successfully retried timestamp from Redis: {}", timeStampToSave);
                    } catch (Exception e) {
                        logger.warn("Retry failed for timestamp from Redis: {}", timestamp);
                    }
                }
            }
        } finally {
            isFlushing = false;
            lock.unlock();
        }
    }

    /**
     * Get all time logs without any filtering or pagination.
     */
    public List<TimeLogDto> getTimeLogs() {
        return TimeLogMapper.map(repository.findAll());
    }

    public boolean getDatabaseAvailabilityStatus() {
        return isDatabaseAvailable;
    }

    public void setDatabaseAvailability(boolean status) {
        this.isDatabaseAvailable = status;
    }
}
