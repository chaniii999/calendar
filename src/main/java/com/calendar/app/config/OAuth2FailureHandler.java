package com.calendar.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
            String errorMessage = "oauth2_login_failed";
            String detailedError = exception.getMessage();
            
            // OAuth2 관련 에러 상세 분석
            if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
                String errorCode = oauth2Exception.getError().getErrorCode();
                log.warn("OAuth2 로그인 실패 - Error Code: {}, Message: {}", errorCode, detailedError, exception);
                
                switch (errorCode) {
                    case "invalid_id_token":
                        errorMessage = "ID 토큰 검증 실패 (시간 동기화 문제일 수 있음)";
                        break;
                    case "invalid_grant":
                        errorMessage = "인증 코드가 만료되었거나 이미 사용됨";
                        break;
                    case "access_denied":
                        errorMessage = "사용자가 로그인을 취소함";
                        break;
                    case "invalid_client":
                        errorMessage = "클라이언트 인증 실패";
                        break;
                    default:
                        errorMessage = "OAuth2 인증 실패: " + errorCode;
                        break;
                }
            } else {
                log.warn("OAuth2 로그인 실패: {}", detailedError, exception);
                errorMessage = "로그인 처리 중 오류가 발생했습니다";
            }
            
            // 프론트엔드로 에러 정보 전달
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String encodedDetails = URLEncoder.encode(detailedError, StandardCharsets.UTF_8);
            String redirectUrl = successRedirect + "?error=" + encodedError + "&details=" + encodedDetails;
            
            log.info("OAuth2 실패, 프론트엔드로 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("OAuth2 실패 처리 중 오류 발생: {}", e.getMessage(), e);
            try {
                response.sendRedirect(successRedirect + "?error=" + URLEncoder.encode("로그인 실패 처리 중 오류가 발생했습니다", StandardCharsets.UTF_8));
            } catch (Exception redirectException) {
                log.error("리다이렉트 실패: {}", redirectException.getMessage(), redirectException);
            }
        }
    }
}

/*
OAuth2 로그인 실패 처리 핸들러
- 사용자가 OAuth2 인증에 실패했을 때 호출됩니다.
- 실패 이유를 로그에 기록하고, 프론트엔드 애플리케이션으로 리다이렉트하여 오류 메시지를 전달합니다.
- 프론트엔드에서 오류 메시지를 받아 사용자에게 알림을 표시할 수 있습니다.

개선사항:
- OAuth2 에러 코드별 상세 분석
- 시간 동기화 문제 감지 및 안내
- 상세한 로깅 추가
- 사용자 친화적인 에러 메시지 제공
*/

