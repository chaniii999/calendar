package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SsePushService {

    private static final long DEFAULT_TIMEOUT_MS = Duration.ofHours(6).toMillis();

    // 사용자별 SSE 연결 관리 (단일 탭 가정, 다중 지원 시 리스트로 확장)
    private final Map<String, SseEmitter> userIdToEmitter = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        userIdToEmitter.put(userId, emitter);
        emitter.onCompletion(() -> userIdToEmitter.remove(userId));
        emitter.onTimeout(() -> userIdToEmitter.remove(userId));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) { }
        return emitter;
    }

    public void pushScheduleReminder(Schedule schedule) {
        String userId = schedule.getUser().getId();
        SseEmitter emitter = userIdToEmitter.get(userId);
        if (emitter == null) {
            log.debug("SSE 미구독 userId={}", userId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name("schedule-reminder")
                .id(schedule.getId())
                .data(Map.of(
                        "scheduleId", schedule.getId(),
                        "title", schedule.getTitle(),
                        "scheduleDate", schedule.getScheduleDate(),
                        "startTime", schedule.getStartTime(),
                        "isAllDay", schedule.isAllDay()
                )));
        } catch (IOException e) {
            log.warn("SSE 전송 실패, 구독 해제 userId={}", userId);
            userIdToEmitter.remove(userId);
        }
    }
}


