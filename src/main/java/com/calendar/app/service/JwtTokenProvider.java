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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("nickname", user.getNickname());

        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getAccessTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getRefreshTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
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

    // 토큰 만료 시간 확인
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true; // 토큰이 유효하지 않으면 만료된 것으로 간주
        }
    }

    // 토큰 만료까지 남은 시간 (초)
    public long getTokenExpirationTime(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            return Math.max(0, (expiration.getTime() - now.getTime()) / 1000);
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }

    // 토큰 갱신 (액세스 토큰이 만료되었거나 곧 만료될 때)
    public String refreshAccessToken(String email) {
        log.debug("액세스 토큰 갱신 요청: {}", email);
        return createAccessToken(email);
    }

}