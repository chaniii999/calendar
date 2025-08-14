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
    private String subtitle; // 일정 소제목
    private String description; // 일정 설명
    private String color; // 캘린더 표시 색상

    // === 일정 날짜/시간 정보 ===
    private LocalDate scheduleDate; // 일정 날짜
    private LocalTime startTime; // 시작 시간
    private LocalTime endTime; // 종료 시간
    private Boolean isAllDay; // 종일 일정 여부
    private Boolean isRecurring; // 반복 일정 여부
    private String recurrenceRule; // 반복 규칙

    // === 학습 관련 정보 ===
    private String studyMode; // 학습 모드
    private Integer plannedStudyMinutes; // 계획된 학습 시간 (분)
    private Integer plannedBreakMinutes; // 계획된 휴식 시간 (분)
    private String studyGoal; // 학습 목표
    private String difficulty; // 예상 난이도

    // === 알림 설정 ===
    private Integer reminderMinutes; // 알림 시간 (분 전)
    private Boolean isReminderEnabled; // 알림 활성화 여부

}