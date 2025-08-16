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

        // Redis에 refresh token 저장
        redisService.saveRefreshToken(email, refresh, jwtTokenProvider.getRefreshTokenExpirationTime());


        try {
            String nameParam = name != null ? URLEncoder.encode(name, StandardCharsets.UTF_8) : "";
            res.sendRedirect(successRedirect + "?u=" + nameParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
