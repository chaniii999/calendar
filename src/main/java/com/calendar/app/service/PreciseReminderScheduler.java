package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
import com.calendar.app.event.ReminderTimeEvent;
import com.calendar.app.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreciseReminderScheduler {

    private final ScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매분 정각에 정확한 알림 시간이 된 일정들을 찾아서 이벤트 발행
     */
    @Scheduled(cron = "0 * * * * *") // 매분 정각
    @Transactional(readOnly = true)
    public void checkReminderTimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        // 현재 시간을 분 단위로 정확히 맞춤 (초는 제거)
        LocalTime currentTime = LocalTime.of(now.getHour(), now.getMinute());
        
        log.debug("알림 시간 체크: {} {}", today, currentTime);
        
        // 현재 시간에 알림이 필요한 일정들 조회
        List<Schedule> dueSchedules = scheduleRepository
                .findByReminderTimeAndNotReminded(today, currentTime);
        
        if (!dueSchedules.isEmpty()) {
            log.info("알림 시간 도달: {}건의 일정", dueSchedules.size());
            
            for (Schedule schedule : dueSchedules) {
                // 이벤트 발행 (비동기 처리)
                ReminderTimeEvent event = new ReminderTimeEvent(
                        this, schedule, now);
                eventPublisher.publishEvent(event);
                
                log.debug("알림 이벤트 발행: scheduleId={}, title={}, startTime={}", 
                        schedule.getId(), schedule.getTitle(), schedule.getStartTime());
            }
        }
    }

    /**
     * 매일 자정에 전날 알림 상태 초기화
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void resetDailyReminders() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int resetCount = scheduleRepository.resetRemindedStatus(yesterday);
        
        if (resetCount > 0) {
            log.info("전날 알림 상태 초기화: {}건", resetCount);
        }
    }
}
