package com.neogroup.timelogs.controller;

import com.neogroup.timelogs.dto.TimeLogDto;
import com.neogroup.timelogs.entity.TimeLog;
import com.neogroup.timelogs.repository.TimeLogRepository;
import com.neogroup.timelogs.service.TimeLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


class TimeControllerIT extends ApiBaseTest {

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private TimeLogService timeLogService;

    @Autowired
    private MockMvc mockMvc;

    private TimeLog currentTimeLog;

    private final HttpHeaders commonHeaders = new HttpHeaders();
    private final ObjectMapper objectMapper = new ObjectMapper();


    @BeforeEach
    public void setup() {
        timeLogRepository.deleteAll();
        currentTimeLog = timeLogRepository.save(
            new TimeLog(LocalDateTime.now()));
    }

    @AfterEach
    public void teardown() {
        timeLogRepository.deleteAll();
    }

    @Test
    void shouldReturnAllLogsTest() throws Exception {

        UriComponentsBuilder uriBuilder = fromUriString("/api/v1/time_logs");
        MvcResult result = mockMvc.perform(
                        get(uriBuilder.build().encode().toUri())
                                .headers(commonHeaders)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<TimeLogDto> responseDtos = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertThat(responseDtos).isNotEmpty();
        Assertions.assertEquals(1, responseDtos.size());
    }

    @Test
    void shouldSaveAndReturnTimeLogsInChronologicalOrderDuringDatabaseDowntimeTest() throws Exception {

        UriComponentsBuilder uriBuilder = fromUriString("/api/v1/time_logs");
        MvcResult result = mockMvc.perform(
                        get(uriBuilder.build().encode().toUri())
                                .headers(commonHeaders)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<TimeLogDto> responseDtos = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertThat(responseDtos).isNotEmpty();
        Assertions.assertEquals(1, responseDtos.size());

        // Simulate database downtime
        getPostgresContainer().stop();
        timeLogService.setDatabaseAvailability(false);

        LocalDateTime now1 = LocalDateTime.now();
        timeLogService.saveTimeLog(now1);

        MvcResult resultTwo = mockMvc.perform(
                        get(uriBuilder.build().encode().toUri())
                                .headers(commonHeaders)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<TimeLogDto> responseDtosTwo = objectMapper.readValue(resultTwo.getResponse().getContentAsString(), new TypeReference<>(){});
        assertThat(responseDtosTwo).isNotEmpty();
        Assertions.assertEquals(1, responseDtosTwo.size());

        // Restore database
        getPostgresContainer().start();

        LocalDateTime now2 = LocalDateTime.now();
        timeLogService.saveTimeLog(now2);

        // Allow retry mechanism to flush cached data
        Thread.sleep(5000);

        MvcResult resultThree = mockMvc.perform(
                        get(uriBuilder.build().encode().toUri())
                                .headers(commonHeaders)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<TimeLogDto> responseDtosThree = objectMapper.readValue(resultThree.getResponse().getContentAsString(), new TypeReference<>(){});
        assertThat(responseDtosThree).isNotEmpty();
        Assertions.assertEquals(3, responseDtosThree.size());
    }
}