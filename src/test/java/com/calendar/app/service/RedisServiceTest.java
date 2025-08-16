package com.calendar.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("리프레시 토큰 저장 성공")
    void saveRefreshToken_Success() {
        // given
        String email = "test@example.com";
        String refreshToken = "test.refresh.token";
        long expirationTime = 604800L;

        // when
        redisService.saveRefreshToken(email, refreshToken, expirationTime);

        // then
        verify(valueOperations, times(1)).set(
                eq("RT:test@example.com"),
                eq(refreshToken),
                eq(expirationTime),
                eq(java.util.concurrent.TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("리프레시 토큰 조회 성공")
    void getRefreshToken_Success() {
        // given
        String email = "test@example.com";
        String expectedToken = "test.refresh.token";
        when(valueOperations.get("RT:test@example.com")).thenReturn(expectedToken);

        // when
        String result = redisService.getRefreshToken(email);

        // then
        assertThat(result).isEqualTo(expectedToken);
        verify(valueOperations, times(1)).get("RT:test@example.com");
    }

    @Test
    @DisplayName("리프레시 토큰 조회 실패 - 토큰 없음")
    void getRefreshToken_NotFound() {
        // given
        String email = "test@example.com";
        when(valueOperations.get("RT:test@example.com")).thenReturn(null);

        // when
        String result = redisService.getRefreshToken(email);

        // then
        assertThat(result).isNull();
        verify(valueOperations, times(1)).get("RT:test@example.com");
    }

    @Test
    @DisplayName("리프레시 토큰 삭제 성공")
    void deleteRefreshToken_Success() {
        // given
        String email = "test@example.com";

        // when
        redisService.deleteRefreshToken(email);

        // then
        verify(redisTemplate, times(1)).delete("RT:test@example.com");
    }

    @Test
    @DisplayName("여러 사용자의 리프레시 토큰 관리")
    void multipleUsersRefreshTokenManagement() {
        // given
        String user1Email = "user1@example.com";
        String user2Email = "user2@example.com";
        String token1 = "token1";
        String token2 = "token2";

        when(valueOperations.get("RT:user1@example.com")).thenReturn(token1);
        when(valueOperations.get("RT:user2@example.com")).thenReturn(token2);

        // when
        String result1 = redisService.getRefreshToken(user1Email);
        String result2 = redisService.getRefreshToken(user2Email);

        // then
        assertThat(result1).isEqualTo(token1);
        assertThat(result2).isEqualTo(token2);
        verify(valueOperations, times(1)).get("RT:user1@example.com");
        verify(valueOperations, times(1)).get("RT:user2@example.com");
    }
}
