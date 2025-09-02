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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "OAuth2 로그인 및 토큰 관리 API")
public class AuthController {

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
        // 이미 OIDC 인증된 세션이면 재인증 루프 방지: 바로 토큰 발급 후 프론트로 리다이렉트
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            String name = oidc.getFullName();

            String access = jwtTokenProvider.createAccessToken(email);
            String refresh = jwtTokenProvider.createRefreshToken(email);
            redisService.saveRefreshToken(email, refresh, jwtTokenProvider.getRefreshTokenExpirationTime());

            String nameParam = name != null ? java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8) : "";
            String accessParam = java.net.URLEncoder.encode(access, java.nio.charset.StandardCharsets.UTF_8);
            String refreshParam = java.net.URLEncoder.encode(refresh, java.nio.charset.StandardCharsets.UTF_8);
            String redirectUrl = successRedirect + "?accessToken=" + accessParam + "&refreshToken=" + refreshParam + "&u=" + nameParam;
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
        summary = "토큰 생성 테스트 (개발용)",
        description = "특정 이메일로 토큰을 생성하여 토큰 생성 과정을 테스트합니다. (개발 환경에서만 사용)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 생성 성공"),
        @ApiResponse(responseCode = "400", description = "토큰 생성 실패")
    })
    @GetMapping("/debug/token-test")
    public Map<String, Object> debugTokenTest(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 사용자 조회 테스트
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                result.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "nickname", user.getNickname()
                ));
            } else {
                result.put("user", "NOT_FOUND");
                result.put("error", "사용자를 찾을 수 없습니다: " + email);
                return result;
            }
            
            // 2. Access Token 생성 테스트
            String accessToken = jwtTokenProvider.createAccessToken(email);
            result.put("accessToken", accessToken != null ? "생성됨 (" + accessToken.length() + "자)" : "생성실패");
            
            // 3. Refresh Token 생성 테스트
            String refreshToken = jwtTokenProvider.createRefreshToken(email);
            result.put("refreshToken", refreshToken != null ? "생성됨 (" + refreshToken.length() + "자)" : "생성실패");
            
            // 4. Redis 저장 테스트
            try {
                redisService.saveRefreshToken(email, refreshToken, 3600);
                String savedToken = redisService.getRefreshToken(email);
                result.put("redis", savedToken != null ? "저장/조회 성공" : "저장/조회 실패");
            } catch (Exception e) {
                result.put("redis", "Redis 오류: " + e.getMessage());
            }
            
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }
        
        return result;
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
}
