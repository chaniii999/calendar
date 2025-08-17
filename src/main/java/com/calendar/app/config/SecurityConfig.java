package com.calendar.app.config;

import com.calendar.app.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:19006", // Expo 웹
                "http://localhost:8081",   // Expo Go
                "http://127.0.0.1:19006",
                "http://192.168.0.7:19006", // 실제 모바일/웹 주소 추가
                "http://192.168.0.7:8081"   // 실제 Expo Go 주소 추가
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    // Spring Security 인증/인가 규칙 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 활성화
                .csrf(AbstractHttpConfigurer::disable) // POST 요청 허용을 위해 CSRF 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안함
                .authorizeHttpRequests(auth -> auth
                        // 인증 관련 엔드포인트는 모두 허용
                        .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/sign-up").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/send-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        // OAuth2 관련 엔드포인트 허용
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        // 테스트 API 허용
                        .requestMatchers(HttpMethod.GET, "/api/timer/test-stats").permitAll()
                        // WebSocket 엔드포인트 허용
                        .requestMatchers("/ws-timer/**").permitAll()
                        // Swagger UI 허용 (개발 환경)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", 
                                       "/swagger-resources/**", "/webjars/**", "/error").permitAll()
                        // 헬스체크 엔드포인트 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
