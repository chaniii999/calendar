package com.calendar.app.repository;

import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;

@Repository
 public interface ScheduleRepository extends JpaRepository<Schedule, String> {

    // 사용자의 특정 날짜 범위 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByUserAndDateRange(@Param("user") User user,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // 사용자의 특정 날짜 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleDate = :date ORDER BY s.startTime")
    List<Schedule> findByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);

    // 사용자의 특정 상태 스케줄 조회
    List<Schedule> findByUserAndStatusOrderByScheduleDateDesc(User user, Schedule.ScheduleStatus status);

    // 사용자의 모든 스케줄 조회 (최신순)
    List<Schedule> findByUserOrderByScheduleDateDescStartTimeAsc(User user);

    // 사용자의 완료된 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.status = 'COMPLETED' ORDER BY s.scheduleDate DESC")
    List<Schedule> findCompletedSchedulesByUser(@Param("user") User user);

    // 사용자의 진행 중인 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.status = 'IN_PROGRESS' ORDER BY s.scheduleDate ASC")
    List<Schedule> findInProgressSchedulesByUser(@Param("user") User user);

    // 사용자의 연기된 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.status = 'POSTPONED' ORDER BY s.scheduleDate ASC")
    List<Schedule> findPostponedSchedulesByUser(@Param("user") User user);

    // 오늘의 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleDate = CURRENT_DATE ORDER BY s.startTime")
    List<Schedule> findTodaySchedules(@Param("user") User user);

    // 이번 주 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleDate BETWEEN :weekStart AND :weekEnd ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findThisWeekSchedules(@Param("user") User user,
                                         @Param("weekStart") LocalDate weekStart,
                                         @Param("weekEnd") LocalDate weekEnd);

    // 이번 달 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleDate BETWEEN :monthStart AND :monthEnd ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findThisMonthSchedules(@Param("user") User user,
                                          @Param("monthStart") LocalDate monthStart,
                                          @Param("monthEnd") LocalDate monthEnd);

    // 알림이 설정된 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isReminderEnabled = true AND s.scheduleDate >= CURRENT_DATE ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findSchedulesWithReminders(@Param("user") User user);

    // 반복 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isRecurring = true ORDER BY s.scheduleDate")
    List<Schedule> findRecurringSchedules(@Param("user") User user);
}
