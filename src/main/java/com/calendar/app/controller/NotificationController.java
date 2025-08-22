package com.calendar.app.controller;

import com.calendar.app.entity.User;
import com.calendar.app.entity.Schedule;
import com.calendar.app.service.ScheduleService;
import com.calendar.app.service.SsePushService;
import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "알림", description = "SSE 구독 및 푸시 알림")
public class NotificationController {

    private final SsePushService ssePushService;
    private final ScheduleService scheduleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public NotificationController(SsePushService ssePushService, ScheduleService scheduleService,
                                  JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.ssePushService = ssePushService;
        this.scheduleService = scheduleService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Operation(summary = "SSE 구독", description = "실시간 알림 수신을 위해 SSE를 구독합니다.")
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal User user) {
        return ssePushService.subscribe(user.getId());
    }

    @Operation(summary = "SSE 구독(쿼리 토큰)", description = "EventSource에서 Authorization 헤더 없이 토큰 쿼리로 구독합니다.")
    @GetMapping("/subscribe-public")
    public SseEmitter subscribeWithToken(@RequestParam("token") String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("invalid token");
        }
        String subject = jwtTokenProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(subject).orElseThrow();
        return ssePushService.subscribe(user.getId());
    }

    // 프론트 호환용 별칭: /api/notifications/stream?token=...
    @Operation(summary = "SSE 스트림(별칭)", description = "subscribe-public과 동일 동작. 프론트 호환용 경로")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam("token") String token) {
        return subscribeWithToken(token);
    }

    @Operation(summary = "테스트 이벤트 발송", description = "클라이언트 연결 확인을 위한 테스트 SSE 이벤트를 발송합니다.")
    @PostMapping("/test")
    public void sendTest(@AuthenticationPrincipal User user, @RequestBody(required = false) java.util.Map<String, String> body) {
        String message = body != null ? body.get("message") : null;
        ssePushService.pushTestEvent(user.getId(), message);
    }

    @Operation(summary = "스케줄 알림 수동 트리거", description = "특정 스케줄의 리마인더 SSE 이벤트를 즉시 발송합니다.")
    @PostMapping("/trigger/{scheduleId}")
    public void triggerReminder(@AuthenticationPrincipal User user, @PathVariable String scheduleId) {
        Schedule schedule = scheduleService.getScheduleEntity(user, scheduleId);
        boolean delivered = ssePushService.pushScheduleReminder(schedule);
        if (!delivered) {
            // 수동 트리거 시에도 미구독이면 reminded를 건드리지 않음
        }
    }
}


