package com.calendar.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 커넥션 누수 방지를 위한 설정 클래스
 * HikariCP 커넥션 풀의 상태를 모니터링하고 관리합니다.
 */
@Configuration
public class ConnectionLeakPreventionConfig {

    @Autowired
    private DataSource dataSource;

    private final AtomicInteger connectionLeakCount = new AtomicInteger(0);

    /**
     * 애플리케이션 시작 시 커넥션 풀 상태 확인
     */
    @Bean
    public CommandLineRunner connectionPoolHealthCheck() {
        return args -> {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                System.out.println("=== HikariCP 커넥션 풀 상태 ===");
                System.out.println("최대 커넥션 수: " + hikariDataSource.getMaximumPoolSize());
                System.out.println("현재 활성 커넥션: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                System.out.println("현재 유휴 커넥션: " + hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                System.out.println("총 커넥션: " + hikariDataSource.getHikariPoolMXBean().getTotalConnections());
                System.out.println("================================");
            }
        };
    }

    /**
     * 주기적으로 커넥션 풀 상태 모니터링 (5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void monitorConnectionPool() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
            int totalConnections = hikariDataSource.getHikariPoolMXBean().getTotalConnections();
            
            // 커넥션 풀 사용률이 80% 이상이면 경고
            if (totalConnections > 0 && (double) activeConnections / totalConnections > 0.8) {
                System.out.println("⚠️  커넥션 풀 사용률이 높습니다: " + 
                    String.format("%.1f%%", (double) activeConnections / totalConnections * 100));
            }
            
            // 커넥션 누수 감지 시 카운트 증가
            if (activeConnections > 0 && connectionLeakCount.get() > 0) {
                System.out.println("🔍 커넥션 누수 감지 횟수: " + connectionLeakCount.get());
            }
        }
    }

    /**
     * 커넥션 누수 감지 시 호출되는 메서드
     */
    public void onConnectionLeakDetected() {
        int count = connectionLeakCount.incrementAndGet();
        System.out.println("🚨 커넥션 누수 감지! 총 " + count + "회 발생");
        
        // 커넥션 풀 상태 로깅
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            System.out.println("현재 활성 커넥션: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
        }
    }

    /**
     * 커넥션 풀 강제 정리 (긴급 상황용)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void forceConnectionPoolCleanup() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // 유휴 커넥션 정리
            hikariDataSource.getHikariPoolMXBean().softEvictConnections();
            System.out.println("🧹 커넥션 풀 정리 완료");
            
            // 누수 카운터 리셋
            connectionLeakCount.set(0);
        }
    }

    /**
     * 커넥션 테스트를 위한 JdbcTemplate 빈
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 커넥션 상태 확인 메서드
     */
    public boolean isConnectionHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5초 타임아웃
        } catch (SQLException e) {
            System.err.println("❌ 커넥션 상태 확인 실패: " + e.getMessage());
            return false;
        }
    }
}
