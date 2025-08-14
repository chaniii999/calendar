package com.calendar.app.repository;

import com.calendar.app.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByKey(String key);
    void deleteByKey(String key); // 선택: 사용하면 한 줄로 삭제 가능
}
