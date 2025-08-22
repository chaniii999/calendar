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
            // 수위 보호: 필드/상태 2중 검증 (리포지토리 조건 누락/변경 대비)
            if (!schedule.isReminderEnabled()) {
                log.debug("skip: reminder disabled scheduleId={}", schedule.getId());
                continue;
            }
            if (schedule.isReminded()) {
                log.debug("skip: already reminded scheduleId={}", schedule.getId());
                continue;
            }

            LocalTime startTime = schedule.getStartTime();
            if (startTime == null) {
                log.debug("skip: no startTime scheduleId={}", schedule.getId());
                continue;
            }

            // 기준 시각: startTime (요청에 따라 reminderMinutes 무시)
            LocalTime triggerTime = startTime;

            // 트리거 시각이 nowTime 이전/같으면 발송
            if (!nowTime.isBefore(triggerTime)) {
                try {
                    log.info("reminder due: scheduleId={} nowTime={} startTime={} title={}",
                            schedule.getId(), nowTime, triggerTime, schedule.getTitle());
                    boolean delivered = ssePushService.pushScheduleReminder(schedule);
                    if (delivered) {
                        schedule.setReminded(true);
                        sent++;
                    } else {
                        log.debug("deliver pending: no active SSE subscriber scheduleId={} userId={}",
                                schedule.getId(), schedule.getUser().getId());
                    }
                } catch (Exception e) {
                    log.error("푸시 알림 전송 실패 scheduleId={}", schedule.getId(), e);
                }
            } else {
                log.debug("not yet: scheduleId={} nowTime={} triggerTime={} title={}",
                        schedule.getId(), nowTime, triggerTime, schedule.getTitle());
            }
        }
        log.debug("sendDueReminders finished today={} nowTime={} sent={}", today, nowTime, sent);
        return sent;
    }
}


