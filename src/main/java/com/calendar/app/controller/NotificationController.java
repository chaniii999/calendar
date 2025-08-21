package com.calendar.app.controller;

import com.calendar.app.entity.User;
import com.calendar.app.service.SsePushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "SSE 구독 및 푸시 알림")
public class NotificationController {

    private final SsePushService ssePushService;

    @Operation(summary = "SSE 구독", description = "실시간 알림 수신을 위해 SSE를 구독합니다.")
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal User user) {
        return ssePushService.subscribe(user.getId());
    }
}


