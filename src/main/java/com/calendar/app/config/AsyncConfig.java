package com.calendar.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Event-");
        executor.initialize();
        return executor;
    }
}

/*
 AsyncConfig.java
 이 클래스는 Spring의 비동기 작업 처리를 위한 설정을 담당합니다.
    @Configuration 어노테이션은 이 클래스가 설정 클래임을 나타내며, @EnableAsync 어노테이션은 비동기 메서드 실행을 활성화합니다.
    eventTaskExecutor() 메서드는 ThreadPoolTaskExecutor를 빈으로 등록하여 비동기 작업을 처리할 스레드 풀을 구성합니다.
    - setCorePoolSize(2): 기본 스레드 수를 2로 설정합니다.
    - setMaxPoolSize(5): 최대 스레드 수를 5로 설정합니다.
    - setQueueCapacity(100): 작업 큐의 용량을 100으로 설정합니다.
    - setThreadNamePrefix("Event-"): 생성되는 스레드의 이름 접두사를 "Event-"로 설정합니다.
    - initialize(): 스레드 풀을 초기화합니다.
 이 설정을 통해 애플리케이션은 비동기 이벤트 처리 작업을 효율적으로 관리할 수 있습니다.
 */