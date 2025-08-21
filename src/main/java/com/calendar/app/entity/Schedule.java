package com.calendar.app.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_schedule_date", columnList = "schedule_date"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Schedule {

    @Id
    @Column(length = 26, updatable = false, nullable = false)
    private String id;

    @JsonIgnore // 순환 참조 방지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user; // 해당 유저


    // === 기본 일정 정보 ===
    @Column(length = 255, nullable = false)
    private String title; // 일정 제목

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description; // 일정 설명

    @Column(length = 7)
    private String color; // 캘린더 표시 색상 (예: "#FF5733")

    // === 일정 날짜/시간 정보 ===
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate; // 일정 날짜

    @Column(name = "start_time")
    private LocalTime startTime; // 시작 시간 (null이면 종일 일정)

    @Column(name = "end_time")
    private LocalTime endTime; // 종료 시간 (null이면 종일 일정)



    // === 알림 설정 ===
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes; // 알림 시간 (분 전)

    @Column(name = "is_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean isReminderEnabled = true; // 알림 활성화 여부

    @Column(name = "reminded", nullable = false)
    @Builder.Default
    private boolean reminded = false; // 시작 시점 알림 발송 완료 여부 (알림 중복 방지)


    // === 메타데이터 ===
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
