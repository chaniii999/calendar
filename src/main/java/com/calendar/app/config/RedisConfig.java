package com.calendar.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.TimeoutOptions;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // 연결 유효성 검사 설정
        template.setEnableDefaultSerializer(false);
        template.setEnableTransactionSupport(false);
        
        return template;
    }
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        
        // Lettuce 클라이언트 설정
        ClientOptions clientOptions = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))
                .autoReconnect(true)
                .build();
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofSeconds(5))
                .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
}

/*
    * RedisConfig.java
    * RedisTemplate 빈을 설정하여 Redis와의 상호작용을 쉽게 함
    * StringRedisSerializer를 사용하여 키와 값을 문자열로 직렬화
    * RedisConnectionFactory를 주입받아 Redis 연결 설정
    * @Bean 어노테이션을 사용하여 RedisTemplate 빈을 생성
    * @Configuration 어노테이션을 사용하여 설정 클래스임을 명시
    * 
    * 개선사항:
    * - 연결 타임아웃 설정 (5초)
    * - 자동 재연결 설정
    * - 명령어 타임아웃 설정
    * - 종료 타임아웃 설정
    * - 읽기 전용 복제본 우선 설정
*/
