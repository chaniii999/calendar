package com.calendar.app.config;

import com.calendar.app.repository.UserRepository;
import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.service.RedisService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OAuth2SuccessHandlerTest {

    @Autowired
    private OAuth2SuccessHandler successHandler;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RedisService redisService;

    @Test
    @DisplayName("OAuth2 성공 시 프론트 리다이렉트 URL 형식 확인")
    void onAuthenticationSuccess_redirectsToFrontend() {
        // given
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("test@example.com");
        when(oidcUser.getFullName()).thenReturn("테스트유저");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        when(jwtTokenProvider.createAccessToken(anyString())).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh");

        HttpServletResponse response = mock(HttpServletResponse.class);

        // when
        successHandler.onAuthenticationSuccess(null, response, authentication);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(1)).sendRedirect(urlCaptor.capture());
        String redirected = urlCaptor.getValue();
        assertThat(redirected).contains("/login/success");
        assertThat(redirected).contains("accessToken=");
        assertThat(redirected).contains("refreshToken=");
    }
}


