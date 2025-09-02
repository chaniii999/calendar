package com.calendar.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";

    public void saveRefreshToken(String email, String refreshToken, long expirationTime) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(key, refreshToken, expirationTime, TimeUnit.SECONDS);
    }

    public String getRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
    }

    /**
     * JWT 토큰을 블랙리스트에 추가합니다.
     */
    public void addToBlacklist(String token, long timeToLive) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", timeToLive, TimeUnit.MILLISECONDS);
    }

    /**
     * JWT 토큰이 블랙리스트에 있는지 확인합니다.
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 블랙리스트에서 토큰을 제거합니다.
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
    }
}