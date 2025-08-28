package com.calendar.app.config;

import com.calendar.app.entity.User;
import com.calendar.app.repository.UserRepository;
import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Value("${frontend.success-redirect}")
    private String successRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) {
        OidcUser oidc = (OidcUser) authentication.getPrincipal();
        String email = oidc.getEmail();
        String name  = oidc.getFullName();

        // upsert
        userRepository.findByEmail(email)
                .map(u -> { if (name != null && !name.isBlank()) u.setNickname(name); return u; })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email).nickname(name != null && !name.isBlank() ? name : email).build()));

        // JWT 생성
        String access = jwtTokenProvider.createAccessToken(email);
        String refresh = jwtTokenProvider.createRefreshToken(email);

        // Redis에 refresh token 저장
        redisService.saveRefreshToken(email, refresh, jwtTokenProvider.getRefreshTokenExpirationTime());


        try {
            String nameParam = name != null ? URLEncoder.encode(name, StandardCharsets.UTF_8) : "";
            String accessParam = URLEncoder.encode(access, StandardCharsets.UTF_8);
            String refreshParam = URLEncoder.encode(refresh, StandardCharsets.UTF_8);
            String redirectUrl = successRedirect + "?accessToken=" + accessParam + "&refreshToken=" + refreshParam + "&u=" + nameParam;
            res.sendRedirect(redirectUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
 */