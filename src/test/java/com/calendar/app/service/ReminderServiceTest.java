package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SsePushService ssePushService;

    @InjectMocks
    private ReminderService reminderService;

    @Test
    @DisplayName("due 알림 1건 전송")
    void sendDue_one() {
        User user = User.builder().id("u1").email("e@e").nickname("n").build();
        Schedule sc = Schedule.builder()
                .id("s1").user(user).title("t").scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9,0)).build();
        when(scheduleRepository.findPendingReminderCandidates(LocalDate.now()))
                .thenReturn(List.of(sc));
        when(ssePushService.pushScheduleReminder(sc)).thenReturn(true);

        int sent = reminderService.sendDueReminders(LocalDate.now().atTime(9, 30));
        assertThat(sent).isEqualTo(1);
    }
}


