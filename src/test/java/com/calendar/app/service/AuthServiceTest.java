package com.calendar.app.service;

import com.calendar.app.dto.auth.TokenDto;
import com.calendar.app.exception.InvalidTokenException;
import com.calendar.app.entity.User;
import com.calendar.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private String validRefreshToken;
    private String validAccessToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        validRefreshToken = "valid.refresh.token";
        validAccessToken = "valid.access.token";
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() {
        // given
        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsername(validRefreshToken)).thenReturn("test@example.com");
        when(redisService.getRefreshToken("test@example.com")).thenReturn(validRefreshToken);
        when(jwtTokenProvider.createAccessToken("test@example.com")).thenReturn(validAccessToken);
        when(jwtTokenProvider.createRefreshToken("test@example.com")).thenReturn("new.refresh.token");
        when(jwtTokenProvider.getRefreshTokenExpirationTime()).thenReturn(604800L);
        when(jwtTokenProvider.getAccessTokenExpirationTime()).thenReturn(3600L);

        // when
        TokenDto result = authService.refreshToken(validRefreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(validAccessToken);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getAccessTokenExpiresIn()).isEqualTo(3600L);

        verify(redisService, times(1)).saveRefreshToken(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refreshToken_InvalidRefreshToken() {
        // given
        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(redisService, never()).getRefreshToken(anyString());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 저장된 토큰과 불일치")
    void refreshToken_TokenMismatch() {
        // given
        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsername(validRefreshToken)).thenReturn("test@example.com");
        when(redisService.getRefreshToken("test@example.com")).thenReturn("different.token");

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token not found or not matched");

        verify(jwtTokenProvider, never()).createAccessToken(anyString());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 토큰 없음")
    void refreshToken_TokenNotFoundInRedis() {
        // given
        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsername(validRefreshToken)).thenReturn("test@example.com");
        when(redisService.getRefreshToken("test@example.com")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token not found or not matched");

        verify(jwtTokenProvider, never()).createAccessToken(anyString());
    }
}
