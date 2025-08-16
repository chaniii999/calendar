package com.calendar.app.service;

import com.calendar.app.config.JwtProperties;
import com.calendar.app.entity.User;
import com.calendar.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        // JWT 설정 모킹
        when(jwtProperties.getSecretKey()).thenReturn("testSecretKey123456789012345678901234567890123456789012345678901234567890");
        when(jwtProperties.getAccessTokenValidityInSeconds()).thenReturn(3600L);
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(604800L);

        // UserRepository 모킹
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void createAccessToken_Success() {
        // when
        String token = jwtTokenProvider.createAccessToken("test@example.com");

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공")
    void createRefreshToken_Success() {
        // when
        String token = jwtTokenProvider.createRefreshToken("test@example.com");

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("토큰에서 사용자명 추출 성공")
    void getUsername_Success() {
        // given
        String token = jwtTokenProvider.createRefreshToken("test@example.com");

        // when
        String username = jwtTokenProvider.getUsername(token);

        // then
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("토큰 유효성 검증 성공")
    void validateToken_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("test@example.com");

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("토큰 유효성 검증 실패 - 잘못된 토큰")
    void validateToken_InvalidToken() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("인증 객체 생성 성공")
    void getAuthentication_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("test@example.com");

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(User.class);
        User user = (User) authentication.getPrincipal();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공")
    void getEmailFromToken_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("test@example.com");

        // when
        String email = jwtTokenProvider.getEmailFromToken(token);

        // then
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("토큰 만료 시간 확인")
    void getTokenExpirationTime_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("test@example.com");

        // when
        long expirationTime = jwtTokenProvider.getTokenExpirationTime(token);

        // then
        assertThat(expirationTime).isGreaterThan(0);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 성공")
    void refreshAccessToken_Success() {
        // given
        String originalToken = jwtTokenProvider.createAccessToken("test@example.com");

        // when
        String newToken = jwtTokenProvider.refreshAccessToken("test@example.com");

        // then
        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(originalToken);
        assertThat(jwtTokenProvider.validateToken(newToken)).isTrue();
    }

    @Test
    @DisplayName("액세스 토큰 생성 실패 - 사용자 없음")
    void createAccessToken_UserNotFound() {
        // given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.createAccessToken("nonexistent@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
