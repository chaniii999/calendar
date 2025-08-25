package com.calendar.app.service;

import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.User;
import com.calendar.app.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SsePushService ssePushService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    @DisplayName("createSchedule 성공")
    void create_ok() {
        User user = User.builder().id("u1").email("e@e").nickname("n").build();
        ScheduleRequest req = ScheduleRequest.builder()
                .title("t").description("d").scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0)).build();

        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScheduleResponse res = scheduleService.createSchedule(user, req);
        assertThat(res.getTitle()).isEqualTo("t");
    }

    @Test
    @DisplayName("getAllSchedules 성공")
    void getAll_ok() {
        User user = User.builder().id("u1").email("e@e").nickname("n").build();
        when(scheduleRepository.findByUserOrderByScheduleDateDescStartTimeAsc(user))
                .thenReturn(java.util.Collections.emptyList());
        List<ScheduleResponse> list = scheduleService.getAllSchedules(user);
        assertThat(list).isEmpty();
    }
}


