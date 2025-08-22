package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
public class SsePushService {

    private static final long DEFAULT_TIMEOUT_MS = Duration.ofHours(6).toMillis();

    // 사용자별 SSE 연결 관리: 다중 탭 지원
    private final Map<String, List<SseEmitter>> userIdToEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        userIdToEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        List<SseEmitter> listAfterSub = userIdToEmitters.get(userId);
        if (listAfterSub != null) {
            log.info("[SSE] sub user={} size={}", userId, listAfterSub.size());
        }
        emitter.onCompletion(() -> {
            List<SseEmitter> list = userIdToEmitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                log.debug("[SSE] unsub(completion) user={} size={}", userId, list.size());
            }
        });
        emitter.onTimeout(() -> {
            List<SseEmitter> list = userIdToEmitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                log.debug("[SSE] unsub(timeout) user={} size={}", userId, list.size());
            }
        });
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) { }
        return emitter;
    }

    public boolean pushScheduleReminder(Schedule schedule) {
        String userId = schedule.getUser().getId();
        List<SseEmitter> emitters = userIdToEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("SSE 미구독 userId={}", userId);
            return false;
        }
        boolean deliveredToAtLeastOne = false;
        int fanout = emitters.size();
        Map<String, Object> payload = Map.of(
                "scheduleId", schedule.getId(),
                "title", schedule.getTitle(),
                "scheduleDate", schedule.getScheduleDate(),
                "startTime", schedule.getStartTime()
        );
        log.info("[SSE] send event=schedule-reminder user={} fanout={} payload={}", userId, fanout, payload);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("schedule-reminder")
                    .id(schedule.getId())
                    .data(payload));
                deliveredToAtLeastOne = true;
            } catch (IOException e) {
                log.warn("[SSE] send fail(remove) user={}", userId);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
                emitters.remove(emitter);
            }
        }
        if (!deliveredToAtLeastOne) {
            log.debug("[SSE] no active client user={} fanout={}", userId, fanout);
        }
        return deliveredToAtLeastOne;
        
    }

    public void pushTestEvent(String userId, String message) {
        List<SseEmitter> emitters = userIdToEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("SSE 미구독 userId={}", userId);
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("test")
                    .data(Map.of(
                            "message", message != null ? message : "test",
                            "ts", System.currentTimeMillis()
                    )));
            } catch (IOException e) {
                log.warn("SSE 전송 실패(개별), emitter 제거 userId={}", userId);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
                emitters.remove(emitter);
            }
        }
    }

    // 주기적 하트비트로 프록시/브라우저 유휴 타임아웃 방지 및 끊긴 연결 정리
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        for (Map.Entry<String, List<SseEmitter>> entry : userIdToEmitters.entrySet()) {
            List<SseEmitter> emitters = entry.getValue();
            if (emitters == null || emitters.isEmpty()) continue;
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("ok"));
                } catch (IOException e) {
                    log.debug("SSE 하트비트 실패, emitter 제거 userId={}", entry.getKey());
                    try { emitter.completeWithError(e); } catch (Exception ignored) {}
                    emitters.remove(emitter);
                }
            }
        }
    }
}

