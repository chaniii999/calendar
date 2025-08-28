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

            // 사용자 저장 또는 업데이트
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

            // 사용자 저장 후 다시 조회하여 최신 정보 확인
            User savedUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자 저장 후 조회 실패: " + email));

            log.debug("JWT 토큰 생성 시작: email={}", email);

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(email);
            String refreshToken = jwtTokenProvider.createRefreshToken(email);

            log.debug("JWT 토큰 생성 완료: accessToken={}, refreshToken={}", 
                    accessToken != null ? "생성됨" : "null", 
                    refreshToken != null ? "생성됨" : "null");

            // Redis에 refresh token 저장
            try {
                redisService.saveRefreshToken(email, refreshToken, jwtTokenProvider.getRefreshTokenExpirationTime());
                log.debug("Redis에 refresh token 저장 완료: email={}", email);
            } catch (Exception e) {
                log.error("Redis에 refresh token 저장 실패: email={}, error={}", email, e.getMessage(), e);
                // Redis 저장 실패해도 토큰은 발급
            }

            // 프론트엔드로 리다이렉트
            String nameParam = name != null ? URLEncoder.encode(name, StandardCharsets.UTF_8) : "";
            String accessParam = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            String refreshParam = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
            String redirectUrl = successRedirect + "?accessToken=" + accessParam + "&refreshToken=" + refreshParam + "&u=" + nameParam;
            
            log.info("OAuth2 로그인 완료, 프론트엔드로 리다이렉트: email={}, redirectUrl={}", email, redirectUrl);
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