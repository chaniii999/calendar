package com.calendar.app.controller;

import com.calendar.app.config.JwtProperties;
import com.calendar.app.entity.RefreshToken;
import com.calendar.app.repository.RefreshTokenRepository;
import com.calendar.app.service.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProps;
    private final RefreshTokenRepository refreshTokenRepository;

    // 프런트에서 이 URL 호출 → Security가 /oauth2/authorization/google 로 리다이렉트
    @GetMapping("/login/google")
    public void login() { /* Security에서 처리 */ }

    @GetMapping("/status")
    public Map<String, Object> status() { return Map.of("ok", true); }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(HttpServletRequest req, HttpServletResponse res) {
        String refresh = readCookie(req, jwtProps.getCookie().getRefresh());
        if (!StringUtils.hasText(refresh)) throw new IllegalArgumentException("refresh token not found");

        String email = jwtTokenProvider.getSubject(refresh);

        // 저장된 refresh 토큰과 일치 확인(토큰 도난 방지)
        RefreshToken rt = refreshTokenRepository.findByKey(email)
                .orElseThrow(() -> new IllegalArgumentException("refresh not found"));
        if (!refresh.equals(rt.getValue())) throw new IllegalArgumentException("invalid refresh");

        String access = jwtTokenProvider.createAccessToken(email);
        addCookie(res, jwtProps.getCookie().getAccess(), access, (int) jwtProps.getAccessValidSeconds());
        return Map.of("accessToken", "rotated");
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest req, HttpServletResponse res) {
        String refresh = readCookie(req, jwtProps.getCookie().getRefresh());
        if (StringUtils.hasText(refresh)) {
            try {
                String email = jwtTokenProvider.getSubject(refresh);
                refreshTokenRepository.findByKey(email)
                        .ifPresent(refreshTokenRepository::delete);
            } catch (Exception ignored) {}
        }
        // 쿠키 삭제
        clearCookie(res, jwtProps.getCookie().getAccess());
        clearCookie(res, jwtProps.getCookie().getRefresh());
        return Map.of("ok", true);
    }

    private String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> name.equals(c.getName()))
                .findFirst().map(Cookie::getValue).orElse(null);
    }

    private void addCookie(HttpServletResponse res, String name, String value, int maxAge) {
        // SameSite/Domain/Secure 설정은 SuccessHandler와 동일하게 헤더로 세팅
        res.addHeader("Set-Cookie",
                name + "=" + value
                        + "; Path=/"
                        + "; Max-Age=" + maxAge
                        + "; HttpOnly"
                        + (jwtProps.getCookie().isSecure() ? "; Secure" : "")
                        + (jwtProps.getCookie().getSameSite() != null ? "; SameSite=" + jwtProps.getCookie().getSameSite() : "")
                        + (jwtProps.getCookie().getDomain() != null ? "; Domain=" + jwtProps.getCookie().getDomain() : ""));
    }

    private void clearCookie(HttpServletResponse res, String name) {
        res.addHeader("Set-Cookie",
                name + "=; Path=/; Max-Age=0; HttpOnly"
                        + (jwtProps.getCookie().isSecure() ? "; Secure" : "")
                        + (jwtProps.getCookie().getSameSite() != null ? "; SameSite=" + jwtProps.getCookie().getSameSite() : "")
                        + (jwtProps.getCookie().getDomain() != null ? "; Domain=" + jwtProps.getCookie().getDomain() : ""));
    }
}
