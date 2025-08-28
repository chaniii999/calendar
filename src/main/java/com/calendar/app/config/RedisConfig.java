package com.calendar.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}

/*
    * RedisConfig.java
    * RedisTemplate 빈을 설정하여 Redis와의 상호작용을 쉽게 함
    * StringRedisSerializer를 사용하여 키와 값을 문자열로 직렬화
    * RedisConnectionFactory를 주입받아 Redis 연결 설정
    * @Bean 어노테이션을 사용하여 RedisTemplate 빈을 생성
    * @Configuration 어노테이션을 사용하여 설정 클래스임을 명시
 */
