package com.calendar.app.service;

import com.calendar.app.config.JwtProperties;
import com.calendar.app.entity.User;
import com.calendar.app.repository.UserRepository;
import jakarta.annotation.PostConstruct;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        log.debug("JWT Properties loaded - Secret Key length: {}",
                jwtProperties.getSecretKey() != null ? jwtProperties.getSecretKey().length() : "null");
        log.debug("Access Token Validity: {} seconds", jwtProperties.getAccessTokenValidityInSeconds());
        log.debug("Refresh Token Validity: {} seconds", jwtProperties.getRefreshTokenValidityInSeconds());

        if (jwtProperties.getSecretKey() == null) {
            throw new IllegalStateException("JWT secret key is not configured properly");
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email) {
        log.debug("Access Token 생성 시작: email={}", email);
        
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("Access Token 생성 실패: 사용자를 찾을 수 없음 - email={}", email);
                        return new IllegalArgumentException("User not found with email: " + email);
                    });

            log.debug("사용자 조회 성공: userId={}, email={}, nickname={}", 
                    user.getId(), user.getEmail(), user.getNickname());

            Map<String, Object> claims = new HashMap<>();
            claims.put("id", user.getId());
            claims.put("email", user.getEmail());
            claims.put("nickname", user.getNickname());

            Date now = new Date();
            Date validity = new Date(now.getTime() + jwtProperties.getAccessTokenValidityInSeconds() * 1000);

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(email)
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

            log.debug("Access Token 생성 완료: email={}, tokenLength={}", email, token.length());
            return token;
            
        } catch (Exception e) {
            log.error("Access Token 생성 중 오류 발생: email={}, error={}", email, e.getMessage(), e);
            throw e;
        }
    }

    public String createRefreshToken(String email) {
        log.debug("Refresh Token 생성 시작: email={}", email);
        
        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + jwtProperties.getRefreshTokenValidityInSeconds() * 1000);

            String token = Jwts.builder()
                    .setSubject(email)
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

            log.debug("Refresh Token 생성 완료: email={}, tokenLength={}", email, token.length());
            return token;
            
        } catch (Exception e) {
            log.error("Refresh Token 생성 중 오류 발생: email={}, error={}", email, e.getMessage(), e);
            throw e;
        }
    }

    public Authentication getAuthentication(String token) {
        String email = getUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰이 만료되었는지 확인
     * @param token JWT 토큰
     * @return 만료 여부
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true; // 파싱 실패 시 만료된 것으로 간주
        }
    }

    /**
     * 토큰의 만료 시간까지 남은 시간(초) 반환
     * @param token JWT 토큰
     * @return 남은 시간(초), 만료되었으면 음수
     */
    public long getTokenExpirationTime(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expiration = claims.getExpiration();
            Date now = new Date();
            return (expiration.getTime() - now.getTime()) / 1000; // 초 단위로 반환
        } catch (JwtException | IllegalArgumentException e) {
            return -1; // 파싱 실패 시 -1 반환
        }
    }

    public long getAccessTokenExpirationTime() {
        return jwtProperties.getAccessTokenValidityInSeconds();
    }

    public long getRefreshTokenExpirationTime() {
        return jwtProperties.getRefreshTokenValidityInSeconds();
    }

    public Map<String, Object> getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token) {
        try {
            return (String) getClaimsFromToken(token).get("email");
        } catch (Exception e) {
            log.error("Error while extracting email from token", e);
            return null;
        }
    }
    
    // 토큰 갱신 (액세스 토큰이 만료되었거나 곧 만료될 때)
    public String refreshAccessToken(String email) {
        log.debug("액세스 토큰 갱신 요청: {}", email);
        return createAccessToken(email);
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인 (기본값: 5분 전)
     * @param token JWT 토큰
     * @param minutesBefore 만료 몇 분 전부터 경고할지 (기본값: 5분)
     * @return 곧 만료될 예정이면 true
     */
    public boolean isTokenExpiringSoon(String token, int minutesBefore) {
        long remainingTime = getTokenExpirationTime(token);
        return remainingTime > 0 && remainingTime <= minutesBefore * 60; // minutesBefore * 60초
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인 (5분 전)
     * @param token JWT 토큰
     * @return 곧 만료될 예정이면 true
     */
    public boolean isTokenExpiringSoon(String token) {
        return isTokenExpiringSoon(token, 5); // 기본값: 5분 전
    }

}