package com.calendar.app.controller;


import com.calendar.app.dto.CommonResponse;
import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import com.calendar.app.exception.ScheduleNotFoundException;
import com.calendar.app.exception.UnauthorizedAccessException;
import com.calendar.app.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "스케줄", description = "일정 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // === CRUD 작업 ===

    @Operation(
        summary = "스케줄 생성",
        description = "새로운 일정을 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "스케줄 생성 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<CommonResponse<ScheduleResponse>> createSchedule(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "스케줄 생성 정보", required = true)
            @RequestBody ScheduleRequest request
    ) {
        ScheduleResponse response = scheduleService.createSchedule(user, request);
        return ResponseEntity.ok(new CommonResponse<>(true, "스케줄이 성공적으로 생성되었습니다.", response));
    }

    @Operation(
        summary = "스케줄 조회",
        description = "특정 일정의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "스케줄 조회 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "404", description = "스케줄을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleResponse>> getSchedule(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "스케줄 ID", required = true, example = "01HXYZ123456789ABCDEF")
            @PathVariable String scheduleId
    ) {
        ScheduleResponse response = scheduleService.getSchedule(user, scheduleId);
        return ResponseEntity.ok(new CommonResponse<>(true, "스케줄 조회 성공", response));
    }

    // 스케줄 수정
    @PutMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId,
            @RequestBody ScheduleRequest request
    ) {
        try {
            ScheduleResponse response = scheduleService.updateSchedule(user, scheduleId, request);
            return ResponseEntity.ok(new CommonResponse<>(true, "스케줄이 성공적으로 수정되었습니다.", response));
        } catch (ScheduleNotFoundException e) {
            log.error("스케줄을 찾을 수 없음", e);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            log.error("권한 없음", e);
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("스케줄 수정 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 수정에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 스케줄 삭제
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId
    ) {
        try {
            scheduleService.deleteSchedule(user, scheduleId);
            return ResponseEntity.ok(new CommonResponse<>(true, "스케줄이 성공적으로 삭제되었습니다.", null));
        } catch (ScheduleNotFoundException e) {
            log.error("스케줄을 찾을 수 없음", e);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            log.error("권한 없음", e);
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("스케줄 삭제 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 삭제에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // === 조회 작업 ===

    @Operation(
        summary = "전체 스케줄 조회",
        description = "사용자의 모든 일정을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "전체 스케줄 조회 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getAllSchedules(
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getAllSchedules(user);
            return ResponseEntity.ok(new CommonResponse<>(true, "전체 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("전체 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 오늘의 스케줄 조회
    @GetMapping("/today")
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getTodaySchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getTodaySchedules(user);
            return ResponseEntity.ok(new CommonResponse<>(true, "오늘의 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("오늘의 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 특정 날짜 스케줄 조회
    @GetMapping("/date/{date}")
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getSchedulesByDate(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getSchedulesByDate(user, date);
            return ResponseEntity.ok(new CommonResponse<>(true, "특정 날짜 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("특정 날짜 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 날짜 범위 스케줄 조회
    @GetMapping("/range")
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getSchedulesByDateRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getSchedulesByDateRange(user, startDate, endDate);
            return ResponseEntity.ok(new CommonResponse<>(true, "날짜 범위 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("날짜 범위 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 완료된 스케줄 조회
    @GetMapping("/completed")
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getCompletedSchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getCompletedSchedules(user);
            return ResponseEntity.ok(new CommonResponse<>(true, "완료된 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("완료된 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 진행 중인 스케줄 조회
    @GetMapping("/in-progress")
    public ResponseEntity<CommonResponse<List<ScheduleResponse>>> getInProgressSchedules(
            @AuthenticationPrincipal User user
    ) {
        try {
            List<ScheduleResponse> schedules = scheduleService.getInProgressSchedules(user);
            return ResponseEntity.ok(new CommonResponse<>(true, "진행 중인 스케줄 조회 성공", schedules));
        } catch (Exception e) {
            log.error("진행 중인 스케줄 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // === 상태 관리 ===

    // 스케줄 상태 변경
    @PutMapping("/{scheduleId}/status")
    public ResponseEntity<CommonResponse<ScheduleResponse>> updateScheduleStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId,
            @RequestParam Schedule.ScheduleStatus status
    ) {
        try {
            ScheduleResponse response = scheduleService.updateScheduleStatus(user, scheduleId, status);
            return ResponseEntity.ok(new CommonResponse<>(true, "스케줄 상태가 성공적으로 변경되었습니다.", response));
        } catch (Exception e) {
            log.error("스케줄 상태 변경 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "스케줄 상태 변경에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 완료율 업데이트
    @PutMapping("/{scheduleId}/completion-rate")
    public ResponseEntity<CommonResponse<ScheduleResponse>> updateCompletionRate(
            @AuthenticationPrincipal User user,
            @PathVariable String scheduleId,
            @RequestParam Integer completionRate
    ) {
        try {
            ScheduleResponse response = scheduleService.updateCompletionRate(user, scheduleId, completionRate);
            return ResponseEntity.ok(new CommonResponse<>(true, "완료율이 성공적으로 업데이트되었습니다.", response));
        } catch (Exception e) {
            log.error("완료율 업데이트 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(false, "완료율 업데이트에 실패했습니다: " + e.getMessage(), null));
        }
    }
}