package com.neogroup.timelogs.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "time_log", indexes = @Index(name = "idx_time", columnList = "time"))
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDateTime time;

    public TimeLog() {
    }

    public TimeLog(LocalDateTime time) {
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TimeLog that = (TimeLog) o;
        return Objects.equals(id, that.id) && Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, time);
    }
}
