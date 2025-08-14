package com.calendar.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "refresh_token",
        indexes = {
                @Index(name = "uk_refresh_token_key", columnList = "token_key", unique = true)
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주의: MySQL 예약어 회피. 컬럼명만 token_key 로, 필드명은 key 유지(기존 코드 호환)
    @Column(name = "token_key", nullable = false, length = 191, unique = true)
    private String key;     // 보통 email 또는 userId

    @Column(name = "token_value", nullable = false, length = 512)
    private String value;   // refresh JWT

    @Column(name = "created_at", nullable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    public RefreshToken(Object o, String email, String refresh) {

        this.key = email; // o는 User 객체로 가정, email 필드 사용
        this.value = refresh; // refresh JWT 값
        this.createdAt = LocalDateTime.now(); // 생성 시점 설정
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
