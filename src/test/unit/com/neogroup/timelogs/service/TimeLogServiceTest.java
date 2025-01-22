package com.neogroup.timelogs.service;

import com.neogroup.timelogs.annotation.Unit;
import com.neogroup.timelogs.entity.TimeLog;
import com.neogroup.timelogs.repository.TimeLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Unit
class TimeLogServiceTest {

    private static final String FAILED_TIME_LOG_KEY = "failedTimeLogs";

    @Mock
    private TimeLogService timeLogService;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private LocalDateTime currentTime;

    @BeforeEach
    public void setUpBeforeEach() {
        currentTime = LocalDateTime.now();
    }

    @AfterEach
    public void teardown() {
        timeLogRepository.deleteAll();
    }

    @Test
    public void saveTimeLogWhenDatabaseIsAvailableTest() {
        // Given
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(new TimeLog(currentTime));

        // When
        timeLogService.saveTimeLog(currentTime);

        // Then
        verify(timeLogRepository, times(1)).save(any(TimeLog.class));
        verify(redisTemplate, times(0)).opsForZSet();
    }

    @Test
    public void saveTimeLogWhenDatabaseIsUnavailableTest() {
        // Given
        timeLogService.setDatabaseAvailability(false);

        // When
        timeLogService.saveTimeLog(currentTime);

        // Then
        verify(timeLogRepository, times(0)).save(any(TimeLog.class));
        verify(redisTemplate, times(1)).opsForZSet();
    }

    @Test
    public void cacheUnsavedTimeLogsTest() {
        // When
        timeLogService.cacheUnsavedTimeLogs(currentTime);

        // Then
        verify(redisTemplate.opsForZSet(), times(1)).add(FAILED_TIME_LOG_KEY, currentTime, currentTime.toEpochSecond(java.time.ZoneOffset.UTC));
    }

    @Test
    public void flushCachedDataWhenRedisContainsLogsTest() {
        // Given
        Set<Object> redisLogs = Set.of(currentTime);
        when(redisTemplate.opsForZSet().range(anyString(), eq(0), eq(-0))).thenReturn(redisLogs);

        // When
        timeLogService.flushCachedData();

        // Then
        verify(timeLogRepository, times(1)).save(any(TimeLog.class));
        verify(redisTemplate.opsForZSet(), times(1)).remove(anyString(), eq(currentTime));
    }

    @Test
    public void flushCachedDataWhenRedisIsEmptyTest() {
        // Given
        when(redisTemplate.opsForZSet().range(anyString(), eq(0), eq(-0))).thenReturn(Set.of());

        // When
        timeLogService.flushCachedData();

        // Then
        verify(timeLogRepository, times(0)).save(any(TimeLog.class));
    }

    @Test
    public void databaseAvailabilityStatusTest() {
        // Given
        timeLogService.setDatabaseAvailability(true);

        // When
        boolean isAvailable = timeLogService.getDatabaseAvailabilityStatus();

        // Then
        assertThat(isAvailable).isTrue();

        // Given
        timeLogService.setDatabaseAvailability(false);

        // When
        isAvailable = timeLogService.getDatabaseAvailabilityStatus();

        // Then
        assertThat(isAvailable).isFalse();
    }

    @Test
    public void flushCachedDataLocksTest() {
        // Given
        timeLogService.setDatabaseAvailability(true);
        timeLogService.flushCachedData();

        // When
        timeLogService.flushCachedData();

        // Then
        verify(timeLogRepository, times(1)).save(any(TimeLog.class));
    }

    @Test
    void saveTimeLogToRedisWhenDbUnavailableTest() {
        doThrow(new RuntimeException("Error during saving data to database")).when(timeLogRepository).save(any(TimeLog.class));

        timeLogService.saveTimeLog(currentTime);

        verify(redisTemplate.opsForZSet(), times(1))
                .add(eq(FAILED_TIME_LOG_KEY), eq(currentTime), anyDouble());
    }

    @Test
    void flushCachedDataWithDbFailureTest() {
        when(redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1))
                .thenReturn(Set.of(currentTime));

        doThrow(new RuntimeException("Database retry error")).when(timeLogRepository).save(any(TimeLog.class));

        timeLogService.flushCachedData();

        verify(redisTemplate.opsForZSet(), never()).remove(FAILED_TIME_LOG_KEY, currentTime);
    }
}