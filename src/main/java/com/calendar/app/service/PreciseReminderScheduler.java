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
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreciseReminderScheduler {

    private final ScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매분 정각에 정확한 알림 시간이 된 일정들을 찾아서 이벤트 발행
     * 타임아웃 설정: 30초 내에 완료되지 않으면 작업 중단
     */
    @Scheduled(cron = "0 * * * * *") // 매분 정각
    @Transactional(readOnly = true, timeout = 30) // 30초 타임아웃
    public void checkReminderTimes() {
        long startTime = System.currentTimeMillis();
        try {
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
                    try {
                        // 이벤트 발행 (비동기 처리)
                        ReminderTimeEvent event = new ReminderTimeEvent(
                                this, schedule, now);
                        eventPublisher.publishEvent(event);
                        
                        log.debug("알림 이벤트 발행: scheduleId={}, title={}, startTime={}", 
                                schedule.getId(), schedule.getTitle(), schedule.getStartTime());
                    } catch (Exception e) {
                        log.error("알림 이벤트 발행 실패: scheduleId={}, error={}", 
                                schedule.getId(), e.getMessage(), e);
                    }
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > 5000) { // 5초 이상 걸리면 경고
                log.warn("알림 시간 체크 작업이 오래 걸림: {}ms", executionTime);
            }
            
        } catch (Exception e) {
            log.error("알림 시간 체크 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 자정에 전날 알림 상태 초기화
     * 타임아웃 설정: 60초 내에 완료되지 않으면 작업 중단
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional(timeout = 60) // 60초 타임아웃
    public void resetDailyReminders() {
        long startTime = System.currentTimeMillis();
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            int resetCount = scheduleRepository.resetRemindedStatus(yesterday);
            
            if (resetCount > 0) {
                log.info("전날 알림 상태 초기화: {}건", resetCount);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > 10000) { // 10초 이상 걸리면 경고
                log.warn("알림 상태 초기화 작업이 오래 걸림: {}ms", executionTime);
            }
            
        } catch (Exception e) {
            log.error("알림 상태 초기화 작업 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 스케줄러 상태 모니터링
     */
    public Map<String, Object> getSchedulerStats() {
        return Map.of(
            "lastCheckTime", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "status", "RUNNING"
        );
    }
}
