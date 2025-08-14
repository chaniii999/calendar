package com.calendar.app.service;


import com.calendar.app.dto.auth.TokenDto;
import com.calendar.app.exception.InvalidTokenException;
import com.calendar.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;



    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // 리프레시 토큰에서 사용자 이메일 추출
        String email = jwtTokenProvider.getUsername(refreshToken);

        // Redis에서 저장된 리프레시 토큰 확인
        String savedRefreshToken = redisService.getRefreshToken(email);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new InvalidTokenException("Refresh token not found or not matched");
        }

        // 새로운 액세스 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);

        // 새로운 리프레시 토큰 발급 (선택사항 - 리프레시 토큰 재사용 정책에 따라)
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

        // Redis에 새로운 리프레시 토큰 저장
        redisService.saveRefreshToken(email, newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpirationTime());

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
