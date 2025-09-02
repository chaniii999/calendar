package com.calendar.app.controller;

import com.calendar.app.dto.CommonResponse;
import com.calendar.app.dto.auth.RefreshTokenRequest;
import com.calendar.app.dto.auth.TokenDto;
import com.calendar.app.service.AuthService;
import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.service.RedisService;
import com.calendar.app.service.SsePushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.calendar.app.repository.UserRepository;
import com.calendar.app.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "OAuth2 로그인 및 토큰 관리 API")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final SsePushService ssePushService;
    private final UserRepository userRepository;

    @Value("${frontend.success-redirect}")
    private String successRedirect;

    @Operation(
        summary = "Google OAuth 로그인",
        description = "Google OAuth2 로그인을 시작합니다. 프론트엔드에서 이 URL을 호출하면 Google 로그인 페이지로 리다이렉트됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Google 로그인 페이지로 리다이렉트"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/login/google")
    public void login(HttpServletResponse response, Authentication authentication) throws IOException {
        // 이미 OIDC 인증된 세션이면 재인증 루프 방지: 세션 기반으로 안전하게 처리
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            String name = oidc.getFullName();

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(email);
            String refreshToken = jwtTokenProvider.createRefreshToken(email);
            
            // Redis에 리프레시 토큰 저장
            redisService.saveRefreshToken(email, refreshToken, jwtTokenProvider.getRefreshTokenExpirationTime());

            // 세션에 토큰 저장 (URL 노출 방지)
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession(true);
            session.setAttribute("accessToken", accessToken);
            session.setAttribute("refreshToken", refreshToken);
            session.setAttribute("userEmail", email);
            session.setAttribute("userName", name);
            session.setAttribute("loginTime", System.currentTimeMillis());

            // 보안: 사용자 이름만 URL에 전달 (토큰은 전달하지 않음)
            String nameParam = name != null ? java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8) : "";
            String redirectUrl = successRedirect + "?u=" + nameParam;
            
            log.info("OAuth2 재인증 완료, 세션 기반으로 프론트엔드로 리다이렉트: email={}", email);
            response.sendRedirect(redirectUrl);
            return;
        }
        // 미인증이면 표준 OAuth2 로그인 시작
        response.sendRedirect("/oauth2/authorization/google");
    }

    @Operation(
        summary = "서버 상태 확인",
        description = "서버가 정상적으로 동작하는지 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서버 정상 동작",
            content = @Content(examples = @ExampleObject(value = "{\"ok\": true}")))
    })
    @GetMapping("/status")
    public Map<String, Object> status() { 
        return Map.of("ok", true); 
    }

    @Operation(
        summary = "상세 헬스체크",
        description = "데이터베이스, Redis, SSE 연결 상태를 포함한 상세한 서버 상태를 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상세 헬스체크 결과"),
        @ApiResponse(responseCode = "503", description = "서비스 일부 비정상")
    })
    @GetMapping("/health")
    public Map<String, Object> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean overallHealth = true;
        
        // 기본 서버 상태
        health.put("server", Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
        
        // 데이터베이스 상태 확인
        try {
            // 간단한 DB 연결 테스트 (JPA가 자동으로 처리)
            health.put("database", Map.of(
                "status", "UP",
                "message", "Database connection successful"
            ));
        } catch (Exception e) {
            health.put("database", Map.of(
                "status", "DOWN",
                "message", "Database connection failed: " + e.getMessage()
            ));
            overallHealth = false;
        }
        
        // Redis 상태 확인
        try {
            redisService.getRefreshToken("health-check-test");
            health.put("redis", Map.of(
                "status", "UP",
                "message", "Redis connection successful"
            ));
        } catch (Exception e) {
            health.put("redis", Map.of(
                "status", "DOWN",
                "message", "Redis connection failed: " + e.getMessage()
            ));
            overallHealth = false;
        }
        
        // SSE 연결 상태
        try {
            Map<String, Object> sseStats = ssePushService.getConnectionStats();
            health.put("sse", Map.of(
                "status", "UP",
                "message", "SSE service operational",
                "stats", sseStats
            ));
        } catch (Exception e) {
            health.put("sse", Map.of(
                "status", "DOWN",
                "message", "SSE service failed: " + e.getMessage()
            ));
            overallHealth = false;
        }
        
        // 전체 상태
        health.put("status", overallHealth ? "UP" : "DOWN");
        health.put("overall", overallHealth);
        
        return health;
    }

    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 형식")
    })
    @PostMapping("/refresh")
    public CommonResponse<?> refreshToken(
        @Parameter(description = "Refresh Token 정보", required = true)
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        TokenDto tokenDto = authService.refreshToken(request.getRefreshToken());
        return new CommonResponse<>(true, "토큰이 성공적으로 갱신되었습니다.", tokenDto);
    }

    @Operation(
        summary = "Google OAuth2 로그인 시작",
        description = "Google OAuth2 로그인을 시작합니다. 브라우저에서 Google 로그인 페이지로 리다이렉트됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Google 로그인 페이지로 리다이렉트")
    })
    @GetMapping("/google-login")
    public void googleLogin(HttpServletResponse response) throws IOException {
        log.info("Google OAuth2 로그인 시작");
        response.sendRedirect("/oauth2/authorization/google");
    }

    @Operation(
        summary = "사용자 정보 조회",
        description = "현재 인증된 사용자의 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자"));
        }

        if (authentication.getPrincipal() instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("email", user.getEmail());
                userInfo.put("nickname", user.getNickname());
                userInfo.put("createdAt", user.getCreatedAt());
                
                return ResponseEntity.ok(userInfo);
            }
        }
        
        return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다"));
    }

    @Operation(
        summary = "로그아웃",
        description = "현재 사용자를 로그아웃하고 모든 인증 정보를 제거합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession(false);
            String userEmail = null;
            String accessToken = null;
            
            if (session != null) {
                userEmail = (String) session.getAttribute("userEmail");
                accessToken = (String) session.getAttribute("accessToken");
                
                if (userEmail != null) {
                    // Redis에서 리프레시 토큰 제거
                    redisService.deleteRefreshToken(userEmail);
                    
                    // JWT 토큰을 블랙리스트에 추가 (선택사항)
                    if (accessToken != null) {
                        // 토큰 만료 시간까지 블랙리스트에 유지
                        long expirationTime = jwtTokenProvider.getExpirationTimeFromToken(accessToken);
                        long currentTime = System.currentTimeMillis();
                        long timeToLive = Math.max(0, expirationTime - currentTime);
                        
                        if (timeToLive > 0) {
                            redisService.addToBlacklist(accessToken, timeToLive);
                            log.info("JWT 토큰을 블랙리스트에 추가: email={}, ttl={}ms", userEmail, timeToLive);
                        }
                    }
                    
                    log.info("사용자 로그아웃 처리 완료: email={}", userEmail);
                }
                
                // 세션 완전 제거
                session.invalidate();
            }
            
            // 인증 관련 쿠키 제거
            clearAuthCookies(response);
            
            // 로그아웃 성공 응답
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "로그아웃되었습니다");
            responseBody.put("redirectUrl", "/"); // 프론트엔드에서 리다이렉트할 URL
            
            return ResponseEntity.ok(responseBody);
            
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "로그아웃 처리 중 오류가 발생했습니다"
            ));
        }
    }

    /**
     * 인증 관련 쿠키를 제거합니다.
     */
    private void clearAuthCookies(HttpServletResponse response) {
        // JSESSIONID 쿠키 제거
        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        
        // CSRF 토큰 쿠키 제거
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", "");
        csrfCookie.setMaxAge(0);
        csrfCookie.setPath("/");
        response.addCookie(csrfCookie);
        
        // 기타 인증 관련 쿠키 제거
        Cookie[] cookies = new Cookie[]{
            new Cookie("remember-me", ""),
            new Cookie("auth-token", ""),
            new Cookie("user-info", "")
        };
        
        for (Cookie cookie : cookies) {
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        
        log.debug("인증 관련 쿠키 제거 완료");
    }

    /**
     * CSRF 토큰 제공 엔드포인트
     * 프론트엔드에서 CSRF 토큰을 요청할 때 사용
     */
    @GetMapping("/api/auth/csrf-token")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfTokenRepository tokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfToken token = tokenRepository.generateToken(request);
        tokenRepository.saveToken(token, request, response);
        
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("csrfToken", token.getToken());
        responseMap.put("headerName", token.getHeaderName());
        responseMap.put("parameterName", token.getParameterName());
        
        return ResponseEntity.ok(responseMap);
    }

    /**
     * 세션에서 JWT 토큰을 안전하게 가져오는 엔드포인트
     * 프론트엔드에서 로그인 후 토큰을 요청할 때 사용
     */
    @GetMapping("/tokens")
    public ResponseEntity<Map<String, Object>> getTokens(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).body(Map.of("error", "세션이 없습니다"));
        }

        String accessToken = (String) session.getAttribute("accessToken");
        String refreshToken = (String) session.getAttribute("refreshToken");
        String userEmail = (String) session.getAttribute("userEmail");
        String userName = (String) session.getAttribute("userName");

        if (accessToken == null || refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "토큰이 없습니다"));
        }

        // 보안: 토큰을 응답 본문에만 포함 (URL에 노출되지 않음)
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("userEmail", userEmail);
        response.put("userName", userName);
        response.put("expiresIn", jwtTokenProvider.getAccessTokenExpirationTime());

        log.info("세션에서 토큰 조회 완료: userEmail={}", userEmail);
        return ResponseEntity.ok(response);
    }
}
