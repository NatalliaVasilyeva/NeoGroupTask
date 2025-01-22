package com.neogroup.timelogs.cron;

import com.neogroup.timelogs.service.DatabaseHealthCheckerService;
import com.neogroup.timelogs.service.TimeLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.shaded.org.awaitility.Durations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(properties = "scheduling.enabled=true")
class TimeLogCronJobTest {

    @Mock
    private TimeLogService timeLogService;

    @Mock
    private DatabaseHealthCheckerService databaseHealthCheckerService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TimeLogCronJob timeLogCronJobForCheckStarting;

    private TimeLogCronJob timeLogCronJob;

    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        timeLogCronJob = new TimeLogCronJob(timeLogService, databaseHealthCheckerService);
        currentTime = LocalDateTime.now();
    }


    @Test
    void logCurrentTimeJobScheduledTest() {
        await()
                .pollDelay(1000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(timeLogCronJobForCheckStarting, atMostOnce()).logCurrentTime());
    }

    @Test
    void checkDatabaseAvailabilityAndFlushJobScheduledTest() {
        await()
                .atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> verify(timeLogCronJobForCheckStarting, atMost(1)).checkDatabaseAvailabilityAndFlush());
    }

    @Test
    void shouldSaveCurrentTimeLogTest() {
        timeLogCronJob.logCurrentTime();

        verify(timeLogService, times(1)).saveTimeLog(argThat(time ->
                time != null && time.isAfter(currentTime.minusSeconds(1)) && time.isBefore(currentTime.plusSeconds(1))
        ));
    }

    @Test
    void shouldCheckDatabaseAvailabilityAndShouldNotFlushWhenDbUnavailableTest() {
        when(timeLogService.getDatabaseAvailabilityStatus()).thenReturn(false);
        when(databaseHealthCheckerService.isDatabaseAvailable()).thenReturn(false);

        timeLogCronJob.checkDatabaseAvailabilityAndFlush();

        verify(databaseHealthCheckerService, times(1)).isDatabaseAvailable();
        verify(timeLogService, never()).flushCachedData();
        verify(timeLogService, times(1)).setDatabaseAvailability(false);
    }

    @Test
    void shouldCheckDatabaseAvailabilityAndFlushWhenDbBecomesAvailableTest() {
        when(timeLogService.getDatabaseAvailabilityStatus()).thenReturn(false);
        when(databaseHealthCheckerService.isDatabaseAvailable()).thenReturn(true);

        timeLogCronJob.checkDatabaseAvailabilityAndFlush();

        verify(databaseHealthCheckerService, times(1)).isDatabaseAvailable();
        verify(timeLogService, times(1)).setDatabaseAvailability(true);
        verify(timeLogService, times(1)).flushCachedData();
    }

    @Test
    void shouldCheckDatabaseAvailabilityAndFlushWhenDbStillAvailableTest() {
        when(timeLogService.getDatabaseAvailabilityStatus()).thenReturn(true);

        timeLogCronJob.checkDatabaseAvailabilityAndFlush();

        verifyNoInteractions(databaseHealthCheckerService);
        verify(timeLogService, never()).flushCachedData();
    }

    @Test
    void shouldCheckDatabaseAvailabilityAndDoNotFlushWhenExceptionOccursTest() {
        when(timeLogService.getDatabaseAvailabilityStatus()).thenReturn(false);
        when(databaseHealthCheckerService.isDatabaseAvailable()).thenThrow(new RuntimeException("Database error"));

        timeLogCronJob.checkDatabaseAvailabilityAndFlush();

        verify(databaseHealthCheckerService, times(1)).isDatabaseAvailable();
        verify(timeLogService, never()).flushCachedData();
        verify(timeLogService, never()).setDatabaseAvailability(anyBoolean());
    }

}