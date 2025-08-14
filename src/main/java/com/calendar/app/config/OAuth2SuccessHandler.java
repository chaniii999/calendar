package com.calendar.app.config;

import com.calendar.app.entity.RefreshToken;
import com.calendar.app.entity.User;
import com.calendar.app.repository.RefreshTokenRepository;
import com.calendar.app.repository.UserRepository;
import com.calendar.app.service.JwtTokenProvider;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProps;

    @Value("${frontend.success-redirect}")
    private String successRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) {
        OidcUser oidc = (OidcUser) authentication.getPrincipal();
        String email = oidc.getEmail();
        String name  = oidc.getFullName();

        // upsert
        User user = userRepository.findByEmail(email)
                .map(u -> { if (name != null && !name.isBlank()) u.setNickname(name); return u; })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email).nickname(name != null && !name.isBlank() ? name : email).build()));

        // JWT 생성
        String access = jwtTokenProvider.createAccessToken(email);
        String refresh = jwtTokenProvider.createRefreshToken(email);

        // 기존 refresh 제거 → 신규 저장
        refreshTokenRepository.findByKey(email).ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(
                new RefreshToken(null, email, refresh) // 엔티티 생성자/빌더에 맞게 수정
        );


        try {
            String nameParam = name != null ? URLEncoder.encode(name, StandardCharsets.UTF_8) : "";
            res.sendRedirect(successRedirect + "?u=" + nameParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
