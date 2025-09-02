package com.calendar.app.config;

import com.calendar.app.service.JwtTokenProvider;
import com.calendar.app.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://everyplan.site",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",    // 추가 포트 지원
                "capacitor://localhost",    // Capacitor 앱
                "ionic://localhost"         // Ionic 앱
        ));
        // 프록시를 통한 요청을 위해 localhost:5173에서의 쿠키 전송 허용
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Content-Type", "New-Access-Token", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, redisService);
    }

    // Spring Security 인증/인가 규칙 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 활성화
                .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        "/api/auth/**",      // 인증 관련 엔드포인트
                        "/oauth2/**",        // OAuth2 관련
                        "/ping",             // 헬스체크
                        "/actuator/**",      // 모니터링
                        "/swagger-ui/**",    // Swagger UI
                        "/v3/api-docs/**"    // API 문서
                    )
                ) // CSRF 보호 활성화 (인증이 필요하지 않은 엔드포인트만 제외)
                .headers(headers -> headers
                    .frameOptions().deny() // X-Frame-Options: DENY
                    .contentTypeOptions() // X-Content-Type-Options: nosniff
                ) // 기본 보안 헤더 추가
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // OAuth2 로그인에 필요한 경우 세션 사용
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // 인증 관련 엔드포인트는 모두 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/ping").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/sign-up").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/login/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/debug/**").permitAll()
                        // SSE 엔드포인트 권한 설정
                        .requestMatchers(HttpMethod.GET, "/api/notifications/subscribe").authenticated() // 세션 기반 (인증 필요)
                        .requestMatchers(HttpMethod.GET, "/api/notifications/subscribe-session").authenticated() // 세션 기반 (인증 필요)
                        .requestMatchers(HttpMethod.GET, "/api/auth/tokens").permitAll() // 토큰 조회 (인증 필요)
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated() // 로그아웃 (인증 필요)
                        .requestMatchers(HttpMethod.GET, "/api/notifications/subscribe-public").permitAll() // 토큰 기반 (호환성 유지)
                        .requestMatchers(HttpMethod.GET, "/api/notifications/stream").permitAll() // 토큰 기반 (호환성 유지)
                        // OAuth2 관련 엔드포인트 허용
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        // Swagger UI 허용 (개발 환경)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", 
                                       "/swagger-resources/**", "/webjars/**", "/error").permitAll()
                        // 헬스체크 엔드포인트 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

/*
SecurityConfig.java
Spring Security 설정 클래스입니다. CORS, CSRF, 세션 관리, 예외 처리, 인증/인가 규칙, OAuth2 로그인 등을 설정합니다.
- CORS 설정: 특정 출처에서의 요청을 허용하고, 쿠키 전송을 허용합니다.
- CSRF 설정: 인증이 필요하지 않은 엔드포인트를 제외하고 CSRF 보호를 활성화합니다.
- 세션 관리: OAuth2 로그인에 필요한 경우에만 세션을 생성합니다.
- 예외 처리: 인증 실패 및 접근 거부 시 커스텀 핸들러를 사용합니다.
- 인증/인가 규칙: 엔드포인트별로 접근 권한을 설정합니다.
- OAuth2 로그인: 성공 및 실패 핸들러를 지정합니다.
- JWT 인증 필터: UsernamePasswordAuthenticationFilter 앞에 JWT 인증 필터를 추가합니다.
 */