package com.neogroup.timelogs.repository;

import com.neogroup.timelogs.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

}
