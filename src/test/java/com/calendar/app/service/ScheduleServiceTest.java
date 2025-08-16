package com.calendar.app.service;

import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import com.calendar.app.exception.ScheduleNotFoundException;
import com.calendar.app.exception.UnauthorizedAccessException;
import com.calendar.app.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private User testUser;
    private Schedule testSchedule;
    private ScheduleRequest testRequest;

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
    }

    @Test
    @DisplayName("스케줄 생성 성공")
    void createSchedule_Success() {
        // given
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        // when
        ScheduleResponse response = scheduleService.createSchedule(testUser, testRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 일정");
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 조회 성공")
    void getSchedule_Success() {
        // given
        when(scheduleRepository.findById("schedule123")).thenReturn(Optional.of(testSchedule));

        // when
        ScheduleResponse response = scheduleService.getSchedule(testUser, "schedule123");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("schedule123");
        assertThat(response.getTitle()).isEqualTo("테스트 일정");
    }

    @Test
    @DisplayName("스케줄 조회 실패 - 스케줄 없음")
    void getSchedule_NotFound() {
        // given
        when(scheduleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.getSchedule(testUser, "nonexistent"))
                .isInstanceOf(ScheduleNotFoundException.class)
                .hasMessageContaining("스케줄을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("스케줄 조회 실패 - 권한 없음")
    void getSchedule_Unauthorized() {
        // given
        User otherUser = User.builder().id("other123").build();
        when(scheduleRepository.findById("schedule123")).thenReturn(Optional.of(testSchedule));

        // when & then
        assertThatThrownBy(() -> scheduleService.getSchedule(otherUser, "schedule123"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("스케줄 수정 성공")
    void updateSchedule_Success() {
        // given
        when(scheduleRepository.findById("schedule123")).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        // when
        ScheduleResponse response = scheduleService.updateSchedule(testUser, "schedule123", testRequest);

        // then
        assertThat(response).isNotNull();
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 삭제 성공")
    void deleteSchedule_Success() {
        // given
        when(scheduleRepository.findById("schedule123")).thenReturn(Optional.of(testSchedule));
        doNothing().when(scheduleRepository).delete(testSchedule);

        // when
        scheduleService.deleteSchedule(testUser, "schedule123");

        // then
        verify(scheduleRepository, times(1)).delete(testSchedule);
    }

    @Test
    @DisplayName("전체 스케줄 조회 성공")
    void getAllSchedules_Success() {
        // given
        List<Schedule> schedules = List.of(testSchedule);
        when(scheduleRepository.findByUserOrderByScheduleDateDescStartTimeAsc(testUser))
                .thenReturn(schedules);

        // when
        List<ScheduleResponse> responses = scheduleService.getAllSchedules(testUser);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("테스트 일정");
    }

    @Test
    @DisplayName("오늘의 스케줄 조회 성공")
    void getTodaySchedules_Success() {
        // given
        List<Schedule> schedules = List.of(testSchedule);
        when(scheduleRepository.findTodaySchedules(testUser)).thenReturn(schedules);

        // when
        List<ScheduleResponse> responses = scheduleService.getTodaySchedules(testUser);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("테스트 일정");
    }

    @Test
    @DisplayName("완료된 스케줄 조회 성공")
    void getCompletedSchedules_Success() {
        // given
        List<Schedule> schedules = List.of(testSchedule);
        when(scheduleRepository.findByUserAndStatusOrderByScheduleDateDesc(testUser, Schedule.ScheduleStatus.COMPLETED))
                .thenReturn(schedules);

        // when
        List<ScheduleResponse> responses = scheduleService.getCompletedSchedules(testUser);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("테스트 일정");
    }

    @Test
    @DisplayName("스케줄 상태 변경 성공")
    void updateScheduleStatus_Success() {
        // given
        when(scheduleRepository.findById("schedule123")).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        // when
        ScheduleResponse response = scheduleService.updateScheduleStatus(testUser, "schedule123", Schedule.ScheduleStatus.COMPLETED);

        // then
        assertThat(response).isNotNull();
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }
}
