package com.neogroup.timelogs.service;

import com.neogroup.timelogs.annotation.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Unit
class DatabaseHealthCheckerServiceTest {

    @Mock
    DataSource postgresDataSource;

    @Mock
    DataSourceHealthIndicator dataSourceHealthIndicator;

    private DatabaseHealthCheckerService databaseHealthCheckerService;

    @BeforeEach
    void setUp() {
        databaseHealthCheckerService = new DatabaseHealthCheckerService(postgresDataSource);
    }

    @Test
    void databaseIsAvailableTest() {
        when(dataSourceHealthIndicator.health()).thenReturn(Health.up().build());

        boolean isAvailable = databaseHealthCheckerService.isDatabaseAvailable();

        assertTrue(isAvailable, "Database is available");
        verify(dataSourceHealthIndicator, times(1)).health();
    }

    @Test
    void databaseIsUnavailableTest() {
        when(dataSourceHealthIndicator.health()).thenReturn(Health.down().build());

        boolean isAvailable = databaseHealthCheckerService.isDatabaseAvailable();

        assertFalse(isAvailable, "Database is unavailable");
        verify(dataSourceHealthIndicator, times(1)).health();
    }

    @Test
    void DatabaseHealthCheckThrowsExceptionTest() {
        when(dataSourceHealthIndicator.health()).thenThrow(new RuntimeException("Health check throws error"));

        boolean isAvailable = databaseHealthCheckerService.isDatabaseAvailable();

        assertFalse(isAvailable, "Database should be unavailable");
        verify(dataSourceHealthIndicator, times(1)).health();
    }
}