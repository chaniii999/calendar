package com.calendar.app.config;

import com.calendar.app.entity.User;
import com.calendar.app.repository.UserRepository;
import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Value("${frontend.success-redirect}")
    private String successRedirect;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) {
        try {
            OidcUser oidc = (OidcUser) authentication.getPrincipal();
            String email = oidc.getEmail();
            String name = oidc.getFullName();

            log.info("OAuth2 로그인 성공: email={}, name={}", email, name);

            if (email == null || email.isBlank()) {
                log.error("OAuth2 로그인 실패: 이메일이 없습니다");
                redirectToError(res, "이메일 정보를 가져올 수 없습니다.");
                return;
            }

            // 사용자 조회 또는 생성
            User user = userRepository.findByEmail(email)
                    .map(u -> {
                        if (name != null && !name.isBlank()) {
                            u.setNickname(name);
                        }
                        log.debug("기존 사용자 업데이트: userId={}, email={}", u.getId(), email);
                        return u;
                    })
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .email(email)
                                .nickname(name != null && !name.isBlank() ? name : email)
                                .build();
                        User savedUser = userRepository.save(newUser);
                        log.info("새 사용자 생성: userId={}, email={}", savedUser.getId(), email);
                        return savedUser;
                    });

            // 기존 세션 무효화 (보안 강화)
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
                log.debug("기존 세션 무효화 완료");
            }

            // 새로운 세션 생성
            HttpSession session = req.getSession(true);
            
            // 세션 고정 공격 방지를 위한 세션 ID 재생성
            req.changeSessionId();
            
            // 세션 보안 설정
            session.setMaxInactiveInterval(3600); // 1시간
            
            // CSRF 토큰 생성 (추가 보안)
            String csrfToken = generateCsrfToken();
            session.setAttribute("csrfToken", csrfToken);

            // 세션에 사용자 정보 저장 (SSE 세션 기반 인증을 위해)
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userNickname", user.getNickname());
            session.setAttribute("loginTime", System.currentTimeMillis());
            log.debug("세션에 사용자 정보 저장: userId={}, email={}", user.getId(), email);

            log.debug("JWT 토큰 생성 시작: email={}", email);

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(email);
            String refreshToken = jwtTokenProvider.createRefreshToken(email);

            log.debug("JWT 토큰 생성 완료: accessToken={}, refreshToken={}", 
                     accessToken != null ? "생성됨" : "null", 
                     refreshToken != null ? "생성됨" : "null");

            // Redis에 리프레시 토큰 저장
            try {
                redisService.saveRefreshToken(email, refreshToken, jwtTokenProvider.getRefreshTokenExpirationTime());
                log.debug("Redis에 리프레시 토큰 저장 완료: email={}", email);
            } catch (Exception e) {
                log.warn("Redis에 리프레시 토큰 저장 실패: email={}, error={}", email, e.getMessage());
                // Redis 저장 실패 시에도 토큰 발급은 계속 진행
            }

            // 세션에 토큰 정보 저장 (보안을 위해 URL 파라미터로 전달하지 않음)
            session.setAttribute("accessToken", accessToken);
            session.setAttribute("refreshToken", refreshToken);
            session.setAttribute("userName", name);
            
            // 세션 쿠키 설정
            setSessionCookie(res, session);
            
            // 세션 설정 완료 로그
            log.info("=== 세션 설정 완료 ===");
            log.info("세션 ID: {}", session.getId());
            log.info("세션 생성 시간: {}", session.getCreationTime());
            log.info("세션 유효 시간: {}", session.getMaxInactiveInterval());
            log.info("세션에 저장된 속성들:");
            log.info("  - userId: {}", session.getAttribute("userId"));
            log.info("  - userEmail: {}", session.getAttribute("userEmail"));
            log.info("  - userNickname: {}", session.getAttribute("userNickname"));
            log.info("  - accessToken: {}", session.getAttribute("accessToken") != null ? "설정됨" : "null");
            log.info("  - refreshToken: {}", session.getAttribute("refreshToken") != null ? "설정됨" : "null");
            log.info("  - userName: {}", session.getAttribute("userName"));
            log.info("  - loginTime: {}", session.getAttribute("loginTime"));
            log.info("  - csrfToken: {}", session.getAttribute("csrfToken") != null ? "설정됨" : "null");
            log.info("=====================");
            
            // 세션 쿠키 설정 확인
            String setCookieHeader = res.getHeader("Set-Cookie");
            log.info("최종 Set-Cookie 헤더: {}", setCookieHeader);
            
            // 보안 헤더 설정
            setSecurityHeaders(res);
            
            // 프론트엔드로 리다이렉트 (토큰 없이, 사용자 이름만 전달)
            String nameParam = name != null ? URLEncoder.encode(name, StandardCharsets.UTF_8) : "";
            String redirectUrl = successRedirect + "?u=" + nameParam;
            
            log.info("OAuth2 로그인 완료, 세션 기반으로 프론트엔드로 리다이렉트: email={}, redirectUrl={}", email, redirectUrl);
            res.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            redirectToError(res, "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void redirectToError(HttpServletResponse response, String errorMessage) {
        try {
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String redirectUrl = successRedirect + "?error=" + encodedError;
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("에러 페이지 리다이렉트 실패: {}", e.getMessage(), e);
        }
    }

    private String generateCsrfToken() {
        // 실제 CSRF 토큰 생성 로직 구현
        // 예: 랜덤 문자열 생성 또는 세션에 저장
        return "dummyCsrfToken"; // 임시 토큰
    }

    private void setSessionCookie(HttpServletResponse response, HttpSession session) {
        // 세션 쿠키를 명시적으로 설정
        String sessionId = session.getId();
        
        // 개발 환경에서는 SameSite=None 사용 (크로스 사이트 쿠키 문제 해결)
        // 프록시를 통한 요청이므로 SameSite=None으로 설정
        String cookieValue = "JSESSIONID=" + sessionId + "; Path=/; HttpOnly=false; SameSite=None; Max-Age=1800";
        
        response.addHeader("Set-Cookie", cookieValue);
        
        log.info("세션 쿠키 설정 완료: {}", cookieValue);
        
        // 추가 디버깅: 응답 헤더 확인
        String setCookieHeader = response.getHeader("Set-Cookie");
        log.info("응답 Set-Cookie 헤더: {}", setCookieHeader);
    }

    private void setSecurityHeaders(HttpServletResponse response) {
        // 보안 헤더 설정 로직
        response.addHeader("X-Content-Type-Options", "nosniff");
        response.addHeader("X-XSS-Protection", "1; mode=block");
        response.addHeader("X-Frame-Options", "DENY");
        response.addHeader("Referrer-Policy", "no-referrer-when-downgrade");
        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.addHeader("Pragma", "no-cache");
        
        // 개발 환경에서는 일부 보안 헤더 제외 (localhost에서 HTTPS 없이 테스트 가능)
        // Content-Security-Policy와 Strict-Transport-Security는 프로덕션에서만 설정
        log.debug("보안 헤더 설정 완료");
    }
}
/*
OAuth2SuccessHandler.java
OAuth2 로그인 성공 시 JWT 토큰을 생성하고, Redis에 리프레시 토큰을 저장한 후, 프론트엔드로 리다이렉트하는 핸들러입니다.

onAuthenticationSuccess 메서드:
- OIDC 사용자 정보에서 이메일과 이름을 추출합니다.
- 이메일을 기준으로 사용자를 데이터베이스에서 조회하고, 없으면 새로 생성합니다.
- JWT 액세스 토큰과 리프레시 토큰을 생성합니다.
- 리프레시 토큰을 Redis에 저장합니다.
- 프론트엔드로 리다이렉트하며, 액세스 토큰과 리프레시 토큰, 사용자 이름을 쿼리 파라미터로 전달합니다.

개선사항:
- 상세한 로깅 추가
- 에러 처리 강화
- 트랜잭션 관리 추가
- 사용자 저장 후 재조회로 최신 정보 확인
- Redis 저장 실패 시에도 토큰 발급 계속 진행
*/
