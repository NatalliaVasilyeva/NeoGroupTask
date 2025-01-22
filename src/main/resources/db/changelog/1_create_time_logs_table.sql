--liquibase formatted sql
--changeset natallia.vasilyeva:1_create_time_logs_table splitStatements:false logicalFilePath:classpath:/db/changelog/1_create_time_logs_table.sql
CREATE TABLE IF NOT EXISTS time_logs
(
    id   BIGSERIAL PRIMARY KEY,
    time TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_time ON time_logs (time);