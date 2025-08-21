package com.calendar.app.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {
    // === 기본 일정 정보 ===
    private String title; // 일정 제목
    private String description; // 일정 설명
    private String color; // 캘린더 표시 색상

    // === 일정 날짜/시간 정보 ===
    private LocalDate scheduleDate; // 일정 날짜
    private LocalTime startTime; // 시작 시간
    private LocalTime endTime; // 종료 시간

    // === 알림 설정 ===
    private Integer reminderMinutes; // 알림 시간 (분 전)
    private Boolean isReminderEnabled; // 알림 활성화 여부

}