package com.calendar.app.controller;

import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import com.calendar.app.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduleService scheduleService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Schedule testSchedule;
    private ScheduleRequest testRequest;
    private ScheduleResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        testSchedule = Schedule.builder()
                .id("schedule123")
                .user(testUser)
                .title("테스트 일정")
                .description("테스트 설명")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        testRequest = ScheduleRequest.builder()
                .title("새 일정")
                .description("새 설명")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        testResponse = ScheduleResponse.builder()
                .id("schedule123")
                .title("테스트 일정")
                .description("테스트 설명")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();
    }

    @Test
    @DisplayName("스케줄 생성 API 성공")
    @WithMockUser(username = "test@example.com")
    void createSchedule_Success() throws Exception {
        // given
        when(scheduleService.createSchedule(any(User.class), any(ScheduleRequest.class)))
                .thenReturn(testResponse);

        // when & then
        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("스케줄이 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.title").value("테스트 일정"));
    }

    @Test
    @DisplayName("스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getSchedule_Success() throws Exception {
        // given
        when(scheduleService.getSchedule(any(User.class), eq("schedule123")))
                .thenReturn(testResponse);

        // when & then
        mockMvc.perform(get("/api/schedule/schedule123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("스케줄 조회 성공"))
                .andExpect(jsonPath("$.data.id").value("schedule123"));
    }

    @Test
    @DisplayName("스케줄 수정 API 성공")
    @WithMockUser(username = "test@example.com")
    void updateSchedule_Success() throws Exception {
        // given
        when(scheduleService.updateSchedule(any(User.class), eq("schedule123"), any(ScheduleRequest.class)))
                .thenReturn(testResponse);

        // when & then
        mockMvc.perform(put("/api/schedule/schedule123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("스케줄이 성공적으로 수정되었습니다."));
    }

    @Test
    @DisplayName("스케줄 삭제 API 성공")
    @WithMockUser(username = "test@example.com")
    void deleteSchedule_Success() throws Exception {
        // given
        doNothing().when(scheduleService).deleteSchedule(any(User.class), eq("schedule123"));

        // when & then
        mockMvc.perform(delete("/api/schedule/schedule123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("스케줄이 성공적으로 삭제되었습니다."));
    }

    @Test
    @DisplayName("전체 스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getAllSchedules_Success() throws Exception {
        // given
        List<ScheduleResponse> responses = List.of(testResponse);
        when(scheduleService.getAllSchedules(any(User.class)))
                .thenReturn(responses);

        // when & then
        mockMvc.perform(get("/api/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("전체 스케줄 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("테스트 일정"));
    }

    @Test
    @DisplayName("오늘의 스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getTodaySchedules_Success() throws Exception {
        // given
        List<ScheduleResponse> responses = List.of(testResponse);
        when(scheduleService.getTodaySchedules(any(User.class)))
                .thenReturn(responses);

        // when & then
        mockMvc.perform(get("/api/schedule/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("오늘의 스케줄 조회 성공"));
    }

    @Test
    @DisplayName("특정 날짜 스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getSchedulesByDate_Success() throws Exception {
        // given
        List<ScheduleResponse> responses = List.of(testResponse);
        when(scheduleService.getSchedulesByDate(any(User.class), any(LocalDate.class)))
                .thenReturn(responses);

        // when & then
        mockMvc.perform(get("/api/schedule/date/2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("특정 날짜 스케줄 조회 성공"));
    }

    @Test
    @DisplayName("날짜 범위 스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getSchedulesByDateRange_Success() throws Exception {
        // given
        List<ScheduleResponse> responses = List.of(testResponse);
        when(scheduleService.getSchedulesByDateRange(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(responses);

        // when & then
        mockMvc.perform(get("/api/schedule/range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("날짜 범위 스케줄 조회 성공"));
    }

    @Test
    @DisplayName("완료된 스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getCompletedSchedules_Success() throws Exception {
        // given
        List<ScheduleResponse> responses = List.of(testResponse);
        when(scheduleService.getCompletedSchedules(any(User.class)))
                .thenReturn(responses);

        // when & then
        mockMvc.perform(get("/api/schedule/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("완료된 스케줄 조회 성공"));
    }

    @Test
    @DisplayName("진행 중인 스케줄 조회 API 성공")
    @WithMockUser(username = "test@example.com")
    void getInProgressSchedules_Success() throws Exception {
        // given
        List<ScheduleResponse> responses = List.of(testResponse);
        when(scheduleService.getInProgressSchedules(any(User.class)))
                .thenReturn(responses);

        // when & then
        mockMvc.perform(get("/api/schedule/in-progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("진행 중인 스케줄 조회 성공"));
    }

    @Test
    @DisplayName("스케줄 상태 변경 API 성공")
    @WithMockUser(username = "test@example.com")
    void updateScheduleStatus_Success() throws Exception {
        // given
        when(scheduleService.updateScheduleStatus(any(User.class), eq("schedule123"), any(Schedule.ScheduleStatus.class)))
                .thenReturn(testResponse);

        // when & then
        mockMvc.perform(put("/api/schedule/schedule123/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("스케줄 상태가 성공적으로 변경되었습니다."));
    }

    @Test
    @DisplayName("완료율 업데이트 API 성공")
    @WithMockUser(username = "test@example.com")
    void updateCompletionRate_Success() throws Exception {
        // given
        when(scheduleService.updateCompletionRate(any(User.class), eq("schedule123"), eq(50)))
                .thenReturn(testResponse);

        // when & then
        mockMvc.perform(put("/api/schedule/schedule123/completion-rate")
                        .param("completionRate", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("완료율이 성공적으로 업데이트되었습니다."));
    }
}
