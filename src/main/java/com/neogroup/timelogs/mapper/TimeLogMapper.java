package com.neogroup.timelogs.mapper;

import com.neogroup.timelogs.dto.TimeLogDto;
import com.neogroup.timelogs.entity.TimeLog;

import java.util.List;
import java.util.stream.Collectors;

public class TimeLogMapper {

    public static TimeLogDto map(TimeLog timeLog) {
        return new TimeLogDto(timeLog.getId() , timeLog.getTime());
    }

    public static List<TimeLogDto> map(List<TimeLog> timeLogs) {
        return timeLogs.stream().map(TimeLogMapper::map)
                .collect(Collectors.toList());
    }
}
