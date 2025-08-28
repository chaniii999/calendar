package com.calendar.app.config;

import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class  JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                if (jwtTokenProvider.validateToken(token)) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // 토큰이 곧 만료될 예정이면 미리 갱신
                    if (jwtTokenProvider.isTokenExpiringSoon(token)) {
                        log.debug("JWT 토큰이 곧 만료될 예정, 미리 갱신: {}", requestURI);
                        String newToken = attemptTokenRefresh(request, response);
                        if (newToken != null) {
                            response.setHeader("New-Access-Token", newToken);
                            log.debug("JWT 토큰 미리 갱신 성공: {}", requestURI);
                        }
                    } else {
                        log.debug("JWT 토큰 검증 성공: {}", requestURI);
                    }
                } else {
                    // 토큰이 만료되었을 때 자동 갱신 시도
                    log.warn("JWT 토큰 만료됨, 자동 갱신 시도: {}", requestURI);
                    String newToken = attemptTokenRefresh(request, response);
                    if (newToken != null) {
                        // 새로운 토큰으로 인증 설정
                        Authentication authentication = jwtTokenProvider.getAuthentication(newToken);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        // 응답 헤더에 새로운 토큰 추가
                        response.setHeader("New-Access-Token", newToken);
                        log.debug("JWT 토큰 자동 갱신 성공: {}", requestURI);
                    } else {
                        log.warn("JWT 토큰 자동 갱신 실패: {}", requestURI);
                    }
                }
            } else {
                log.debug("JWT 토큰이 없음: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰 만료 시 자동 갱신 시도
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 새로운 액세스 토큰 또는 null
     */
    private String attemptTokenRefresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 만료된 토큰에서 이메일 추출 시도
            String expiredToken = resolveToken(request);
            if (expiredToken == null) {
                return null;
            }

            // 만료된 토큰에서 이메일 추출 (만료되어도 클레임은 읽을 수 있음)
            String email = jwtTokenProvider.getEmailFromToken(expiredToken);
            if (email == null) {
                log.warn("만료된 토큰에서 이메일 추출 실패");
                return null;
            }

            // Redis에서 리프레시 토큰 확인
            String refreshToken = redisService.getRefreshToken(email);
            if (refreshToken == null) {
                log.warn("Redis에서 리프레시 토큰을 찾을 수 없음: {}", email);
                return null;
            }

            // 리프레시 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.warn("리프레시 토큰이 유효하지 않음: {}", email);
                return null;
            }

            // 새로운 액세스 토큰 생성
            String newAccessToken = jwtTokenProvider.refreshAccessToken(email);
            log.debug("액세스 토큰 자동 갱신 완료: {}", email);
            
            return newAccessToken;

        } catch (Exception e) {
            log.error("토큰 자동 갱신 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
}

/*
* JwtAuthenticationFilter.java
* Jwt토큰을 받아 인증하는 필터 로직
*
* void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
* - 요청에서 JWT 토큰을 추출하고 검증하는 메서드
* - 토큰이 유효하면 인증 객체를 생성하여 SecurityContext에 저장
* - 토큰이 곧 만료될 예정이면 미리 갱신
* - 토큰이 만료되면 자동으로 갱신을 시도
* - 예외 발생 시 SecurityContext를 초기화하고 로그 기록
* - 마지막에 필터 체인을 계속 진행
*
* String resolveToken(HttpServletRequest request)
* - 요청 헤더에서 "Authorization" 헤더를 읽어 "Bearer " 접두어가 붙은 토큰을 추출
* - 토큰이 없거나 형식이 올바르지 않으면 null 반환
*
* String attemptTokenRefresh(HttpServletRequest request, HttpServletResponse response)
* - 토큰이 만료되었을 때 자동으로 갱신을 시도하는 메서드
* - 만료된 토큰에서 이메일을 추출하고 Redis에서 리프레시 토큰을 확인
* - 리프레시 토큰이 유효하면 새로운 액세스 토큰을 생성하여 반환
*/