package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
import com.calendar.app.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ScheduleRepository scheduleRepository;
    private final SsePushService ssePushService;

    @Transactional
    public int sendDueReminders(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        List<Schedule> candidates = scheduleRepository.findPendingReminderCandidates(today);
        int sent = 0;
        for (Schedule schedule : candidates) {
            // 기준 시각: startTime - reminderMinutes(기본 5분)
            Integer reminderMins = schedule.getReminderMinutes() != null ? schedule.getReminderMinutes() : 5;
            LocalTime triggerTime = schedule.getStartTime().minusMinutes(reminderMins);

            // 트리거 시각이 nowTime 이전/같으면 발송
            if (!nowTime.isBefore(triggerTime)) {
                try {
                    ssePushService.pushScheduleReminder(schedule);
                    schedule.setReminded(true);
                    sent++;
                } catch (Exception e) {
                    log.error("푸시 알림 전송 실패 scheduleId={}", schedule.getId(), e);
                }
            }
        }
        return sent;
    }
}


