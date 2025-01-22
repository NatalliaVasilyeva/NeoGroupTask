package com.neogroup.timelogs.controller;

import com.neogroup.timelogs.TimeLogApplication;
import com.neogroup.timelogs.annotation.Integration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;


@AutoConfigureWebTestClient(timeout = "10000000")
@Integration
@SpringBootTest(
    classes = {
        TimeLogApplication.class,
        TestConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "scheduling.enabled=false")
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:/application-test.yaml")
public abstract class ApiBaseTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("time-log-test")
            .withUsername("postgres")
            .withPassword("postgres");
//            .withInitScript("init.sql");

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:6")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.contexts", () -> "!prod");
        registry.add("spring.liquibase.url", () -> postgresContainer.getJdbcUrl());
        registry.add("spring.liquibase.user", () -> postgresContainer.getUsername());
        registry.add("spring.password.password", () -> postgresContainer.getPassword());
    }


    @BeforeAll
    static void beforeAll() {
        postgresContainer.setWaitStrategy(
            new LogMessageWaitStrategy()
                .withRegEx(".*database system is ready to accept connections.*\\s")
                .withTimes(1)
                .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS))
        );
        postgresContainer.start();
    }

    @AfterAll
    static void afterAll() {
    }

    public GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }

    public PostgreSQLContainer<?> getPostgresContainer() {
        return postgresContainer;
    }
}