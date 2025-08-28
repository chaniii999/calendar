package com.calendar.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;
    
    // ⚠️ 이 클래스는 더 이상 사용되지 않습니다. PreciseReminderScheduler를 사용하세요.

    // ⚠️ 폴링 방식 비활성화 - 이벤트 기반 방식으로 대체
    // @Scheduled(fixedDelay = 30000L)
    public void scanAndPushReminders() {
        log.warn("폴링 방식은 비활성화되었습니다. 이벤트 기반 방식을 사용하세요.");
        // LocalDateTime now = LocalDateTime.now();
        // int sent = reminderService.sendDueReminders(now);
        // if (sent > 0) {
        //     log.info("푸시 알림 전송: {}건", sent);
        // } else {
        //     log.debug("푸시 알림 전송 건수 0건 at {}", now);
        // }
    }
}


