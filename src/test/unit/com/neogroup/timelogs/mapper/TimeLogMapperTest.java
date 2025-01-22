package com.neogroup.timelogs.mapper;

import com.neogroup.timelogs.annotation.Unit;
import com.neogroup.timelogs.entity.TimeLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Unit
public class TimeLogMapperTest {

    @Test
    void shouldReturnCorrectDto() {
        var timeLog = new TimeLog(LocalDateTime.now());

        var timeLogDto = TimeLogMapper.map(timeLog);

        assertEquals(timeLog.getTime(), timeLogDto.time());
    }

    @Test
    void shouldReturnCorrectEntities() {
        var timeLogFirst = new TimeLog(LocalDateTime.now());

        var timeLogSecond = new TimeLog(LocalDateTime.now());

        var timeLogs = TimeLogMapper.map(List.of(timeLogFirst, timeLogSecond));

        assertThat(timeLogs).hasSize(2);
        var timeLogOne = timeLogs.get(0);
        assertEquals(timeLogOne.time(), timeLogFirst.getTime());

        var timeLogTwo = timeLogs.get(1);
        assertEquals(timeLogTwo.time(), timeLogSecond.getTime());
    }

}
