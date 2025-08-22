package com.calendar.app.service;


import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.dto.schedule.ScheduleResponse;
import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import com.calendar.app.exception.ScheduleNotFoundException;
import com.calendar.app.exception.UnauthorizedAccessException;
import com.calendar.app.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SsePushService ssePushService;

    // 공통 권한 검증 메서드
    private Schedule validateScheduleOwnership(User user, String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("스케줄을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("해당 스케줄에 접근할 권한이 없습니다.");
        }

        return schedule;
    }

    // === CRUD 작업 ===

    // 스케줄 생성
    @Transactional
    public ScheduleResponse createSchedule(User user, ScheduleRequest request) {
        log.debug("스케줄 생성 요청 - 사용자: {}, 제목: {}", user.getNickname(), request.getTitle());



        Schedule schedule = Schedule.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .color(request.getColor())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reminderMinutes(request.getReminderMinutes())
                .isReminderEnabled(request.getIsReminderEnabled() != null ? request.getIsReminderEnabled() : true)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.debug("스케줄 생성 완료 - ID: {}", savedSchedule.getId());

        return ScheduleResponse.from(savedSchedule);
    }

    // 스케줄 조회 (단일)
    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(User user, String scheduleId) {
        log.debug("스케줄 조회 요청 - 사용자: {}, 스케줄 ID: {}", user.getNickname(), scheduleId);
        Schedule schedule = validateScheduleOwnership(user, scheduleId);
        return ScheduleResponse.from(schedule);
    }

    // 엔티티 직접 조회 (권한 검증 포함) - 컨트롤러 내 수동 트리거용
    @Transactional(readOnly = true)
    public Schedule getScheduleEntity(User user, String scheduleId) {
        return validateScheduleOwnership(user, scheduleId);
    }

    // 스케줄 수정
    @Transactional
    public ScheduleResponse updateSchedule(User user, String scheduleId, ScheduleRequest request) {
        log.debug("스케줄 수정 요청 - 사용자: {}, 스케줄 ID: {}", user.getNickname(), scheduleId);
        Schedule schedule = validateScheduleOwnership(user, scheduleId);


        // 스케줄 정보 업데이트
        schedule.setTitle(request.getTitle());
        schedule.setDescription(request.getDescription());
        schedule.setColor(request.getColor());
        LocalDate oldDate = schedule.getScheduleDate();
        java.time.LocalTime oldStart = schedule.getStartTime();
        Integer oldReminderMins = schedule.getReminderMinutes();
        // 변경 이전 플래그 보존(필요 시 로깅/분기 활용)
        // Boolean oldEnabled = schedule.isReminderEnabled();

        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setReminderMinutes(request.getReminderMinutes());

        // 일정 날짜/시작시간/알림분 변경 시 reminded 초기화
        if (!java.util.Objects.equals(oldDate, schedule.getScheduleDate())
                || !java.util.Objects.equals(oldStart, schedule.getStartTime())
                || !java.util.Objects.equals(oldReminderMins, schedule.getReminderMinutes())) {
            schedule.setReminded(false);
        }

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.debug("스케줄 수정 완료 - ID: {}", updatedSchedule.getId());

        return ScheduleResponse.from(updatedSchedule);
    }

    // 알림 허용 유무 전용 업데이트 (상세 페이지에서만 변경)
    @Transactional
    public ScheduleResponse updateReminderEnabled(User user, String scheduleId, boolean enabled) {
        log.debug("알림 허용 변경 - 사용자: {}, 스케줄 ID: {}, enabled: {}", user.getNickname(), scheduleId, enabled);
        Schedule schedule = validateScheduleOwnership(user, scheduleId);

        Boolean oldEnabled = schedule.isReminderEnabled();
        schedule.setReminderEnabled(enabled);

        // 활성화 상태 변경 시 다음 트리거를 위해 reminded 초기화
        if (!java.util.Objects.equals(oldEnabled, enabled)) {
            schedule.setReminded(false);
        }

        Schedule updated = scheduleRepository.save(schedule);
        return ScheduleResponse.from(updated);
    }

    // 스케줄 삭제
    @Transactional
    public void deleteSchedule(User user, String scheduleId) {
        log.debug("스케줄 삭제 요청 - 사용자: {}, 스케줄 ID: {}", user.getNickname(), scheduleId);
        Schedule schedule = validateScheduleOwnership(user, scheduleId);

        scheduleRepository.delete(schedule);
        log.debug("스케줄 삭제 완료 - ID: {}", scheduleId);
    }

    // === 조회 작업 ===

    // 사용자의 모든 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules(User user) {
        log.debug("전체 스케줄 조회 요청 - 사용자: {}", user.getNickname());

        List<Schedule> schedules = scheduleRepository.findByUserOrderByScheduleDateDescStartTimeAsc(user);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 날짜 범위 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        log.debug("날짜 범위 스케줄 조회 요청 - 사용자: {}, 기간: {} ~ {}", user.getNickname(), startDate, endDate);

        List<Schedule> schedules = scheduleRepository.findByUserAndDateRange(user, startDate, endDate);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 날짜 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByDate(User user, LocalDate date) {
        log.debug("특정 날짜 스케줄 조회 요청 - 사용자: {}, 날짜: {}", user.getNickname(), date);

        List<Schedule> schedules = scheduleRepository.findByUserAndDate(user, date);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 오늘의 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getTodaySchedules(User user) {
        log.debug("오늘의 스케줄 조회 요청 - 사용자: {}", user.getNickname());

        List<Schedule> schedules = scheduleRepository.findTodaySchedules(user);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }



    // === 알림: 시작 시각 수동 트리거 ===
    @Transactional
    public ScheduleResponse triggerStartReminder(User user, String scheduleId) {
        Schedule schedule = validateScheduleOwnership(user, scheduleId);

        if (!schedule.isReminderEnabled()) {
            throw new IllegalStateException("알림이 비활성화된 스케줄입니다.");
        }
        if (schedule.getStartTime() == null) {
            throw new IllegalArgumentException("시작 시간이 없는 스케줄은 트리거할 수 없습니다.");
        }

        LocalDateTime startDateTime = schedule.getScheduleDate().atTime(schedule.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDateTime)) {
            throw new IllegalStateException("시작 시각 이전에는 트리거할 수 없습니다.");
        }
        if (schedule.isReminded()) {
            return ScheduleResponse.from(schedule);
        }

        boolean delivered = ssePushService.pushScheduleReminder(schedule);
        if (delivered) {
            schedule.setReminded(true);
        }

        Schedule updated = scheduleRepository.save(schedule);
        return ScheduleResponse.from(updated);
    }

    // === 알림: 토글 ===
    @Transactional
    public ScheduleResponse toggleReminderEnabled(User user, String scheduleId) {
        Schedule schedule = validateScheduleOwnership(user, scheduleId);

        boolean newEnabled = !schedule.isReminderEnabled();
        schedule.setReminderEnabled(newEnabled);
        schedule.setReminded(false);

        Schedule updated = scheduleRepository.save(schedule);
        log.debug("알림 토글 - 스케줄 ID: {}, enabled: {}", updated.getId(), newEnabled);
        return ScheduleResponse.from(updated);
    }
}