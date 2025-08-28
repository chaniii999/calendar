package com.calendar.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${frontend.success-redirect}")
    private String successRedirect;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        try {
            String msg = exception.getMessage() != null ? exception.getMessage() : "oauth2_login_failed";
            log.warn("OAuth2 로그인 실패: {}", msg, exception);
            // 실패 시에도 프론트로 리다이렉트하여 메시지 표시
            String redirectUrl = successRedirect + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

/*
OAuth2 로그인 실패 처리 핸들러
- 사용자가 OAuth2 인증에 실패했을 때 호출됩니다.
- 실패 이유를 로그에 기록하고, 프론트엔드 애플리케이션으로 리다이렉트하여 오류 메시지를 전달합니다.
- 프론트엔드에서 오류 메시지를 받아 사용자에게 알림을 표시할 수 있습니다.
 */

