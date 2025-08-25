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

    // 매 30초마다 확인 (운영에서는 1분 권장). cron표현 가능: @Scheduled(cron = "0 * * * * *")
    @Scheduled(fixedDelay = 30000L)
    public void scanAndPushReminders() {
        LocalDateTime now = LocalDateTime.now();
        int sent = reminderService.sendDueReminders(now);
        if (sent > 0) {
            log.info("푸시 알림 전송: {}건", sent);
        } else {
            log.debug("푸시 알림 전송 건수 0건 at {}", now);
        }
    }
}


