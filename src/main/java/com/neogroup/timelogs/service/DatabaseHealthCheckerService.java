package com.neogroup.timelogs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseHealthCheckerService extends AbstractHealthIndicator {

    private final DataSource postgresDataSource;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthCheckerService.class);

    @Autowired
    public DatabaseHealthCheckerService(DataSource postgresDataSource) {
        this.postgresDataSource = postgresDataSource;
    }

    public DataSourceHealthIndicator dbHealthIndicator() {
        DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(postgresDataSource);
        return indicator;
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Health h = dbHealthIndicator().health();
        Status status = h.getStatus();
        if (status != null && "DOWN".equals(status.getCode())) {
            builder.down();
        } else {
            builder.up();
        }
    }

    public boolean isDatabaseAvailable() {
        Health health = dbHealthIndicator().health();
        Status status = health.getStatus();
        return status == null || !"DOWN".equals(status.getCode());
    }
}