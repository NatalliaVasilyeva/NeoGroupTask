package com.neogroup.timelogs.service;

import com.neogroup.timelogs.entity.TimeLog;
import com.neogroup.timelogs.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TimeLogServiceIT extends BaseServiceTest {

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private TimeLogService timeLogService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String FAILED_TIME_LOG_KEY = "failedTimeLogs";

    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUpBeforeEach() {
        testTimestamp = LocalDateTime.now();
        timeLogRepository.deleteAll();
        redisTemplate.delete(FAILED_TIME_LOG_KEY);
    }

    @Test
    void shouldSaveTimeLogToDatabaseTest() {
        TimeLog timeLog = new TimeLog(testTimestamp);

        timeLogRepository.save(timeLog);

        assertThat(timeLogRepository.findAll()).hasSize(1);
        TimeLog retrievedLog = timeLogRepository.findAll().getFirst();
        assertThat(retrievedLog.getTime()).isEqualTo(testTimestamp);
    }

    @Test
    void shouldSaveTimeLogToRedisTest() {
        double score = testTimestamp.toEpochSecond(java.time.ZoneOffset.UTC);

        redisTemplate.opsForZSet().add(FAILED_TIME_LOG_KEY, testTimestamp, score);

        Set<Object> redisLogs = redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1);
        assertThat(redisLogs).isNotNull();
        assertThat(redisLogs).hasSize(1);
        assertThat(LocalDateTime.parse((String) redisLogs.iterator().next())).isEqualTo(testTimestamp);
    }

    @Test
    void shouldRetrieveTimeLogFromRedisAndSaveToDatabaseTest() {
        double score = testTimestamp.toEpochSecond(java.time.ZoneOffset.UTC);
        redisTemplate.opsForZSet().add(FAILED_TIME_LOG_KEY, testTimestamp, score);

        Set<Object> redisLogs = redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1);
        assertThat(redisLogs).isNotNull();
        assertThat(redisLogs).hasSize(1);

        LocalDateTime retrievedTimestamp = LocalDateTime.parse((String)redisLogs.iterator().next());
        timeLogRepository.save(new TimeLog(retrievedTimestamp));

        assertThat(timeLogRepository.findAll()).hasSize(1);
        TimeLog retrievedLog = timeLogRepository.findAll().getFirst();
        assertThat(retrievedLog.getTime()).isEqualTo(retrievedTimestamp);

        redisTemplate.opsForZSet().remove(FAILED_TIME_LOG_KEY, retrievedTimestamp);

        redisLogs = redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1);
        assertThat(redisLogs).isEmpty();
    }

    @Test
    void shouldSaveToRedisWhenDatabaseUnavailableTest() {
        getPostgresContainer().stop();
        timeLogService.setDatabaseAvailability(false);

        timeLogService.saveTimeLog(testTimestamp);

        Set<Object> redisLogs = redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1);
        assertThat(redisLogs).isNotNull();
        assertThat(LocalDateTime.parse((String)redisLogs.iterator().next())).isEqualTo(testTimestamp);
    }

    @Test
    void shouldFlushFromRedisToDatabaseWhenDBBecomeAvailableTest() {
        redisTemplate.opsForZSet().add(FAILED_TIME_LOG_KEY, testTimestamp, testTimestamp.toEpochSecond(ZoneOffset.UTC));

        getPostgresContainer().start();

        timeLogService.flushCachedData();

        List<TimeLog> savedLogs = timeLogRepository.findAll();
        assertThat(savedLogs).hasSize(1);
        assertThat(savedLogs.getFirst().getTime()).isEqualTo(testTimestamp);

        Set<Object> redisLogs = redisTemplate.opsForZSet().range(FAILED_TIME_LOG_KEY, 0, -1);
        assertThat(redisLogs).isEmpty();
    }
}
