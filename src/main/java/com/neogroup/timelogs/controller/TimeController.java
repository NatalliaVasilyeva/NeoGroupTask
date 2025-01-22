package com.neogroup.timelogs.controller;

import com.neogroup.timelogs.dto.TimeLogDto;
import com.neogroup.timelogs.service.TimeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TimeController {
    private final TimeLogService service;

    @Autowired
    public TimeController(TimeLogService service) {
        this.service = service;
    }

    @GetMapping("/time_logs")
    @ResponseStatus(HttpStatus.OK)
    public List<TimeLogDto> getAllTimeLogs() {
        return service.getTimeLogs();
    }
}
