package com.calendar.app.dto.schedule;

import com.calendar.app.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    // === 기본 정보 ===
    private String id;
    private String title;
    private String description;
    private String color;

    // === 날짜/시간 정보 ===
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // === 알림 설정 ===
    private Integer reminderMinutes;
    private boolean isReminderEnabled;

    // === 메타데이터 ===
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // === 정적 팩토리 메서드 ===
    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .color(schedule.getColor())
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .reminderMinutes(schedule.getReminderMinutes())
                .isReminderEnabled(schedule.isReminderEnabled())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}