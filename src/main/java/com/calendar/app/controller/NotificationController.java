package com.calendar.app.controller;

import com.calendar.app.entity.User;
import com.calendar.app.entity.Schedule;
import com.calendar.app.service.ScheduleService;
import com.calendar.app.service.SsePushService;
import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@AllArgsConstructor
@Tag(name = "알림", description = "SSE 구독 및 푸시 알림")
public class NotificationController {

    private final SsePushService ssePushService;
    private final ScheduleService scheduleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Operation(summary = "SSE 구독 (세션 기반)", description = "세션 기반 인증으로 실시간 알림을 구독합니다. 토큰 노출 없이 안전합니다.")
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal User user, HttpSession session) {
        if (user == null) {
            throw new IllegalArgumentException("인증이 필요합니다");
        }
        
        // 세션 보안 검증
        validateSessionSecurity(session, user);
        
        log.info("SSE 구독 시작: userId={}, email={}", user.getId(), user.getEmail());
        return ssePushService.subscribe(user.getId());
    }

    @Operation(summary = "SSE 구독 (세션 기반 - 대안)", description = "HttpSession을 직접 사용하여 SSE를 구독합니다.")
    @GetMapping("/subscribe-session")
    public SseEmitter subscribeWithSession(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            // 세션에 userId가 없으면 현재 인증된 사용자에서 추출
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                userId = user.getId();
                // 세션에 저장
                session.setAttribute("userId", userId);
            } else {
                throw new IllegalArgumentException("인증이 필요합니다");
            }
        }
        
        // 세션 보안 검증
        validateSessionSecurity(session, null);
        
        log.info("SSE 구독 시작 (세션): userId={}", userId);
        return ssePushService.subscribe(userId);
    }

    @Operation(summary = "토큰 조회 (세션 기반)", description = "세션에서 토큰을 안전하게 조회합니다.")
    @GetMapping("/tokens")
    public ResponseEntity<Map<String, Object>> getTokens(HttpSession session, HttpServletRequest request) {
        try {
            log.info("=== 토큰 조회 요청 시작 ===");
            log.info("요청 URL: {}", request.getRequestURL());
            log.info("요청 메서드: {}", request.getMethod());
            log.info("세션 ID: {}", session != null ? session.getId() : "null");
            log.info("세션 생성 시간: {}", session != null ? session.getCreationTime() : "null");
            log.info("세션 마지막 접근 시간: {}", session != null ? session.getLastAccessedTime() : "null");
            log.info("세션 유효 시간: {}", session != null ? session.getMaxInactiveInterval() : "null");
            
            // 쿠키 정보 상세 로깅
            log.info("=== 쿠키 정보 ===");
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    log.info("쿠키: {} = {} (Domain: {}, Path: {}, HttpOnly: {}, Secure: {})", 
                            cookie.getName(), cookie.getValue(), cookie.getDomain(), 
                            cookie.getPath(), cookie.isHttpOnly(), cookie.getSecure());
                }
            } else {
                log.warn("요청에 쿠키가 없습니다!");
            }
            log.info("==================");
            
            // 세션에 저장된 모든 속성 로그
            if (session != null) {
                java.util.Enumeration<String> attributeNames = session.getAttributeNames();
                while (attributeNames.hasMoreElements()) {
                    String name = attributeNames.nextElement();
                    Object value = session.getAttribute(name);
                    log.info("세션 속성 - {}: {}", name, value != null ? value.toString() : "null");
                }
            }
            
            // 요청 헤더 로그
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                log.info("요청 헤더 - {}: {}", headerName, headerValue);
            }
            
            // 세션 유효성 검증
            if (session == null || session.getAttribute("userId") == null) {
                log.warn("세션이 없거나 userId가 없습니다. session={}, userId={}", 
                        session != null ? "존재" : "null", 
                        session != null ? session.getAttribute("userId") : "null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "세션이 만료되었습니다. 다시 로그인해주세요."));
            }
            
            // CSRF 토큰 검증 (선택적)
            String requestCsrfToken = request.getHeader("X-CSRF-TOKEN");
            String sessionCsrfToken = (String) session.getAttribute("csrfToken");
            if (requestCsrfToken != null && sessionCsrfToken != null && 
                !requestCsrfToken.equals(sessionCsrfToken)) {
                log.warn("CSRF 토큰 불일치: userId={}", session.getAttribute("userId"));
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "보안 토큰이 유효하지 않습니다."));
            }
            
            String accessToken = (String) session.getAttribute("accessToken");
            String refreshToken = (String) session.getAttribute("refreshToken");
            
            log.info("세션에서 토큰 조회 - accessToken: {}, refreshToken: {}", 
                    accessToken != null ? "존재" : "null", 
                    refreshToken != null ? "존재" : "null");
            
            if (accessToken == null) {
                log.warn("세션에 accessToken이 없습니다. userId={}", session.getAttribute("userId"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "세션에 토큰이 없습니다. 다시 로그인해주세요."));
            }
            
            // 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(accessToken)) {
                log.warn("만료된 액세스 토큰: userId={}", session.getAttribute("userId"));
                
                // 리프레시 토큰으로 갱신 시도
                if (refreshToken != null) {
                    try {
                        // refreshToken에서 email 추출
                        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
                        String newAccessToken = jwtTokenProvider.refreshAccessToken(email);
                        if (newAccessToken != null) {
                            session.setAttribute("accessToken", newAccessToken);
                            accessToken = newAccessToken;
                            log.info("액세스 토큰 갱신 성공: userId={}", session.getAttribute("userId"));
                        } else {
                            log.warn("액세스 토큰 갱신 실패: userId={}", session.getAttribute("userId"));
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "토큰이 만료되었습니다. 다시 로그인해주세요."));
                        }
                    } catch (Exception e) {
                        log.error("토큰 갱신 실패: userId={}, error={}", session.getAttribute("userId"), e.getMessage());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "토큰 갱신에 실패했습니다. 다시 로그인해주세요."));
                    }
                } else {
                    log.warn("리프레시 토큰이 없어 갱신할 수 없습니다: userId={}", session.getAttribute("userId"));
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "토큰이 만료되었습니다. 다시 로그인해주세요."));
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            if (refreshToken != null) {
                response.put("refreshToken", refreshToken);
            }
            response.put("userId", session.getAttribute("userId"));
            response.put("userEmail", session.getAttribute("userEmail"));
            response.put("userNickname", session.getAttribute("userNickname"));
            
            log.info("토큰 조회 성공: userId={}", session.getAttribute("userId"));
            log.info("=== 토큰 조회 요청 완료 ===");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("토큰 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "토큰 조회 중 오류가 발생했습니다."));
        }
    }

    @Operation(summary = "SSE 구독(쿼리 토큰)", description = "EventSource에서 Authorization 헤더 없이 토큰 쿼리로 구독합니다. (기존 호환성 유지)")
    @GetMapping("/subscribe-public")
    public SseEmitter subscribeWithToken(@RequestParam("token") String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }
        String subject = jwtTokenProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(subject).orElseThrow(() -> 
            new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        log.info("SSE 구독 시작 (토큰): userId={}, email={}", user.getId(), user.getEmail());
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
    public ResponseEntity<Map<String, String>> sendTest(@AuthenticationPrincipal User user, 
                                                       @RequestBody(required = false) Map<String, String> body,
                                                       HttpSession session) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "인증이 필요합니다"));
        }
        
        // 세션 보안 검증
        validateSessionSecurity(session, user);
        
        String message = body != null ? body.get("message") : "테스트 메시지";
        ssePushService.pushTestEvent(user.getId(), message);
        
        log.info("테스트 이벤트 발송: userId={}, message={}", user.getId(), message);
        return ResponseEntity.ok(Map.of("message", "테스트 이벤트가 발송되었습니다"));
    }

    @Operation(summary = "스케줄 알림 수동 트리거", description = "특정 스케줄의 리마인더 SSE 이벤트를 즉시 발송합니다.")
    @PostMapping("/trigger/{scheduleId}")
    public ResponseEntity<Map<String, String>> triggerReminder(@AuthenticationPrincipal User user, 
                                                               @PathVariable String scheduleId,
                                                               HttpSession session) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "인증이 필요합니다"));
        }
        
        // 세션 보안 검증
        validateSessionSecurity(session, user);
        
        try {
            Schedule schedule = scheduleService.getScheduleEntity(user, scheduleId);
            boolean delivered = ssePushService.pushScheduleReminder(schedule);
            
            if (delivered) {
                log.info("수동 알림 트리거 성공: userId={}, scheduleId={}", user.getId(), scheduleId);
                return ResponseEntity.ok(Map.of("message", "알림이 성공적으로 발송되었습니다"));
            } else {
                log.warn("수동 알림 트리거 실패 (구독자 없음): userId={}, scheduleId={}", user.getId(), scheduleId);
                return ResponseEntity.ok(Map.of("message", "현재 구독자가 없어 알림을 발송할 수 없습니다"));
            }
        } catch (Exception e) {
            log.error("수동 알림 트리거 중 오류: userId={}, scheduleId={}, error={}", 
                     user.getId(), scheduleId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "알림 발송 중 오류가 발생했습니다"));
        }
    }

    /**
     * 세션 보안 검증
     */
    private void validateSessionSecurity(HttpSession session, User user) {
        if (session == null) {
            throw new IllegalArgumentException("세션이 없습니다");
        }
        
        // 세션 타임아웃 검증
        Long loginTime = (Long) session.getAttribute("loginTime");
        if (loginTime != null) {
            long currentTime = System.currentTimeMillis();
            long sessionAge = currentTime - loginTime;
            long maxSessionAge = 3600 * 1000; // 1시간
            
            if (sessionAge > maxSessionAge) {
                log.warn("세션 만료: userId={}, sessionAge={}ms", 
                        session.getAttribute("userId"), sessionAge);
                session.invalidate();
                throw new IllegalArgumentException("세션이 만료되었습니다. 다시 로그인해주세요.");
            }
        }
        
        // 사용자 ID 일치성 검증
        if (user != null) {
            String sessionUserId = (String) session.getAttribute("userId");
            if (sessionUserId != null && !sessionUserId.equals(user.getId())) {
                log.warn("세션 사용자 ID 불일치: sessionUserId={}, currentUserId={}", 
                        sessionUserId, user.getId());
                throw new IllegalArgumentException("세션 정보가 일치하지 않습니다");
            }
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "세션 무효화 및 쿠키 삭제를 통한 로그아웃 처리")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session, HttpServletResponse response) {
        try {
            log.info("=== 로그아웃 요청 시작 ===");
            log.info("세션 ID: {}", session.getId());
            
            // 세션 무효화
            session.invalidate();
            log.info("세션 무효화 완료");
            
            // JSESSIONID 쿠키 삭제 (명시적으로 만료시킴)
            String cookieValue = "JSESSIONID=; Path=/; HttpOnly=false; Max-Age=0; SameSite=None";
            response.addHeader("Set-Cookie", cookieValue);
            log.info("JSESSIONID 쿠키 삭제 완료: {}", cookieValue);
            
            // Spring Session 쿠키도 삭제 (있다면)
            String sessionCookieValue = "SESSION=; Path=/; HttpOnly=false; Max-Age=0; SameSite=None";
            response.addHeader("Set-Cookie", sessionCookieValue);
            log.info("SESSION 쿠키 삭제 완료: {}", sessionCookieValue);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "로그아웃 완료");
            
            log.info("=== 로그아웃 요청 완료 ===");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "로그아웃 처리 실패");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}


