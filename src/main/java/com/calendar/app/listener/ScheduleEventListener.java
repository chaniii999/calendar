package com.calendar.app.listener;

import com.calendar.app.event.ScheduleCreatedEvent;
import com.calendar.app.event.ScheduleUpdatedEvent;
import com.calendar.app.event.ReminderTimeEvent;
import com.calendar.app.service.SsePushService;
import com.calendar.app.service.ReminderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleEventListener {

    private final SsePushService ssePushService;
    private final ReminderStatusService reminderStatusService;

    /**
     * 스케줄 생성 시 즉시 알림 처리
     */
    @EventListener
    @Async
    public void handleScheduleCreated(ScheduleCreatedEvent event) {
        var schedule = event.getSchedule();
        
        if (schedule.isReminderEnabled() && schedule.getStartTime() != null) {
            log.info("새 스케줄 생성 알림 처리: scheduleId={}, title={}", 
                    schedule.getId(), schedule.getTitle());
            
            // 즉시 알림 전송 (테스트용)
            ssePushService.pushTestEvent(schedule.getUser().getId(), 
                    "새 일정이 생성되었습니다: " + schedule.getTitle());
        }
    }

    /**
     * 스케줄 수정 시 알림 처리
     */
    @EventListener
    @Async
    public void handleScheduleUpdated(ScheduleUpdatedEvent event) {
        var schedule = event.getSchedule();
        
        if (schedule.isReminderEnabled() && schedule.getStartTime() != null) {
            log.info("스케줄 수정 알림 처리: scheduleId={}, title={}", 
                    schedule.getId(), schedule.getTitle());
            
            // 수정 알림 전송 (테스트용)
            ssePushService.pushTestEvent(schedule.getUser().getId(), 
                    "일정이 수정되었습니다: " + schedule.getTitle());
        }
    }

    /**
     * 알림 시간 도달 시 즉시 알림 전송
     */
    @EventListener
    @Async
    public void handleReminderTime(ReminderTimeEvent event) {
        var schedule = event.getSchedule();
        
        log.info("알림 시간 도달: scheduleId={}, title={}, reminderTime={}", 
                schedule.getId(), schedule.getTitle(), event.getReminderTime());
        
        // 즉시 알림 전송
        boolean delivered = ssePushService.pushScheduleReminder(schedule);
        
        if (delivered) {
            log.info("알림 전송 성공: scheduleId={}", schedule.getId());
            // 알림 전송 성공 시 reminded 상태를 true로 설정
            reminderStatusService.markAsReminded(schedule.getId());
        } else {
            log.warn("알림 전송 실패 (구독자 없음): scheduleId={}", schedule.getId());
        }
    }
}

/*
스케줄 이벤트 리스너
- 스케줄 생성, 수정, 알림 시간 도달 이벤트를 비동기로 처리
- SSE 푸시 서비스와 연동하여 사용자에게 실시간 알림 전송
- 알림 전송 성공 시 DB에 reminded 상태 업데이트
- 비동기 처리로 메인 스레드 부하 최소화
- 향후 알림 실패 재시도 로직 추가 가능
 */