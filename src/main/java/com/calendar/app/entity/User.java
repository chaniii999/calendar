package com.calendar.app.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 26, updatable = false, nullable = false)
    private String id;

    @Column(length = 40, nullable = false, unique = true)
    private String email;

    @Column(length = 15, nullable = false)
    private String nickname;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // 최근 접속일자
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // UserDetails 구현 메서드
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_USER");
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UlidCreator.getUlid().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
