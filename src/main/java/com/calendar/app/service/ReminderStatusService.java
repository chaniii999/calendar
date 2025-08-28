package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
import com.calendar.app.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderStatusService {

    private final ScheduleRepository scheduleRepository;

    /**
     * 알림 전송 완료 후 상태 업데이트
     */
    @Transactional
    public void markAsReminded(String scheduleId) {
        scheduleRepository.findById(scheduleId).ifPresent(schedule -> {
            schedule.setReminded(true);
            scheduleRepository.save(schedule);
            log.debug("알림 상태 업데이트: scheduleId={}, reminded=true", scheduleId);
        });
    }

    /**
     * 알림 상태 초기화 (스케줄 수정 시)
     */
    @Transactional
    public void resetReminderStatus(String scheduleId) {
        scheduleRepository.findById(scheduleId).ifPresent(schedule -> {
            schedule.setReminded(false);
            scheduleRepository.save(schedule);
            log.debug("알림 상태 초기화: scheduleId={}, reminded=false", scheduleId);
        });
    }
}
