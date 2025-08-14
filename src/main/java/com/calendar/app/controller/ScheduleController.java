package com.calendar.app.controller;


import com.calendar.app.dto.ApiResponse;
import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import com.calendar.app.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // === CRUD 작업 ===

    // 스케줄 생성
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal User user,
            @RequestBody ScheduleRequest request
    ) {
        try {
            ScheduleResponse response = scheduleService.createSchedule(user, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "스케줄이 성공적으로 생성되었습니다.", response));
        } catch (Exception e) {
            log.error("스케줄 생성 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 생성에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 스케줄 조회 (단일)
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId
    ) {
        try {
            ScheduleResponse response = scheduleService.getSchedule(user, scheduleId);
            return ResponseEntity.ok(new ApiResponse<>(true, "스케줄 조회 성공", response));
        } catch (Exception e) {
            log.error("스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 스케줄 수정
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId,
            @RequestBody ScheduleRequest request
    ) {
        try {
            ScheduleResponse response = scheduleService.updateSchedule(user, scheduleId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "스케줄이 성공적으로 수정되었습니다.", response));
        } catch (Exception e) {
            log.error("스케줄 수정 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 수정에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 스케줄 삭제
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId
    ) {
        try {
            scheduleService.deleteSchedule(user, scheduleId);
            return ResponseEntity.ok(new ApiResponse<>(true, "스케줄이 성공적으로 삭제되었습니다.", null));
        } catch (Exception e) {
            log.error("스케줄 삭제 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 삭제에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // === 조회 작업 ===

    // 전체 스케줄 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getAllSchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getAllSchedules(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "전체 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("전체 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 오늘의 스케줄 조회
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getTodaySchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getTodaySchedules(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "오늘의 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("오늘의 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 특정 날짜 스케줄 조회
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByDate(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getSchedulesByDate(user, date);
            return ResponseEntity.ok(new ApiResponse<>(true, "특정 날짜 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("특정 날짜 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 날짜 범위 스케줄 조회
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByDateRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getSchedulesByDateRange(user, startDate, endDate);
            return ResponseEntity.ok(new ApiResponse<>(true, "날짜 범위 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("날짜 범위 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 완료된 스케줄 조회
    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getCompletedSchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getCompletedSchedules(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "완료된 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("완료된 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 진행 중인 스케줄 조회
    @GetMapping("/in-progress")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getInProgressSchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getInProgressSchedules(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "진행 중인 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("진행 중인 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // === 상태 관리 ===

    // 스케줄 상태 변경
    @PutMapping("/{scheduleId}/status")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateScheduleStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId,
            @RequestParam Schedule.ScheduleStatus status
    ) {
        try {
            ScheduleResponse response = scheduleService.updateScheduleStatus(user, scheduleId, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "스케줄 상태가 성공적으로 변경되었습니다.", response));
        } catch (Exception e) {
            log.error("스케줄 상태 변경 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "스케줄 상태 변경에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 완료율 업데이트
    @PutMapping("/{scheduleId}/completion-rate")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateCompletionRate(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId,
            @RequestParam Integer completionRate
    ) {
        try {
            ScheduleResponse response = scheduleService.updateCompletionRate(user, scheduleId, completionRate);
            return ResponseEntity.ok(new ApiResponse<>(true, "완료율이 성공적으로 업데이트되었습니다.", response));
        } catch (Exception e) {
            log.error("완료율 업데이트 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "완료율 업데이트에 실패했습니다: " + e.getMessage(), null));
        }
    }
}