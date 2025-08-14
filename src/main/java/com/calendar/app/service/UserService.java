package com.calendar.app.service;

import com.calendar.app.dto.auth.TokenDto;
import com.calendar.app.entity.RefreshToken;
import com.calendar.app.entity.User;
import com.calendar.app.repository.RefreshTokenRepository;
import com.calendar.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OAuth(OIDC) + JWT 기반 회원가입(Upsert) / 로그인 서비스
 * - registerOrUpdateFromOidc: OIDC 프로필로 사용자 Upsert
 * - loginWithOidc: Upsert 후 Access/Refresh 발급 및 Refresh 저장
 *
 * 쿠키 세팅/삭제는 Controller(or SuccessHandler)에서 처리하십시오.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * OIDC 프로필로 사용자 Upsert (회원가입/프로필 동기화)
     * - key: email
     */
    @Transactional
    public User registerOrUpdateFromOidc(OidcUser oidcUser) {
        final String email = oidcUser.getEmail();
        final String name  = safeTrim(oidcUser.getFullName());
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OIDC email이 없습니다.");
        }

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isPresent()) {
            User u = found.get();
            // 필요한 범위에서만 안전하게 동기화
            if (name != null && !name.isBlank()) {
                u.setNickname(name);
            }
            // 예) u.setProfileImageUrl(oidcUser.getPicture());
            return u; // Dirty Checking으로 업데이트
        }

        // 신규 생성
        User created = User.builder()
                .email(email)
                .nickname(name != null && !name.isBlank() ? name : email)
                // provider / providerId(sub) 필드가 있으면 함께 세팅 권장
                .build();

        return userRepository.save(created);
    }

    /**
     * OIDC 로그인: Upsert 후 Access/Refresh 발급 + Refresh 저장
     * - Access: FE에서 Authorization 헤더로 사용
     * - Refresh: DB에 저장(쿠키는 Controller에서 HttpOnly로 세팅)
     */
    @Transactional
    public LoginResponseDto loginWithOidc(OidcUser oidcUser) {
        User user = registerOrUpdateFromOidc(oidcUser);

        String accessToken  = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        // 기존 Refresh 제거 후 신규 저장
        refreshTokenRepository.findByKey(user.getEmail())
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken rt = RefreshToken.builder()
                .key(user.getEmail())
                .value(refreshToken)
                .build();
        refreshTokenRepository.save(rt);

        TokenDto tokenDto = TokenDto.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpirationTime())
                .build();

        return LoginResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .token(tokenDto)
                .message("Google OAuth 로그인 완료 (JWT 발급)")
                .build();
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
