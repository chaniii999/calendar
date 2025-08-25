package com.calendar.app.controller;

import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.User;
import com.calendar.app.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ScheduleControllerTest.MockConfig.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("스케줄 생성 200")
    void createSchedule_ok() throws Exception {
        ScheduleResponse response = ScheduleResponse.builder()
                .id("s1").title("t").description("d")
                .scheduleDate(LocalDate.now()).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0))
                .build();
        when(scheduleService.createSchedule(any(User.class), any(ScheduleRequest.class))).thenReturn(response);

        ScheduleRequest req = ScheduleRequest.builder()
                .title("t").description("d")
                .scheduleDate(LocalDate.now()).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0))
                .build();

        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 스케줄 조회 200")
    void getAll_ok() throws Exception {
        ScheduleResponse response = ScheduleResponse.builder()
                .id("s1").title("t").description("d")
                .scheduleDate(LocalDate.now()).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0))
                .build();
        when(scheduleService.getAllSchedules(any(User.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/schedule"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        ScheduleService scheduleService() {
            return Mockito.mock(ScheduleService.class);
        }
    }
}


