package com.calendar.app.exception;

import com.calendar.app.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException 처리")
    void handleIllegalArgument() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 인수입니다");

        // when
        ResponseEntity<ApiResponse> response = exceptionHandler.handleIllegalArgument(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 인수입니다");
    }

    @Test
    @DisplayName("ScheduleNotFoundException 처리")
    void handleScheduleNotFound() {
        // given
        ScheduleNotFoundException exception = new ScheduleNotFoundException("스케줄을 찾을 수 없습니다");

        // when
        ResponseEntity<ApiResponse> response = exceptionHandler.handleScheduleNotFound(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("스케줄을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("UnauthorizedAccessException 처리")
    void handleUnauthorizedAccess() {
        // given
        UnauthorizedAccessException exception = new UnauthorizedAccessException("권한이 없습니다");

        // when
        ResponseEntity<ApiResponse> response = exceptionHandler.handleUnauthorizedAccess(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("권한이 없습니다");
    }

    @Test
    @DisplayName("InvalidCompletionRateException 처리")
    void handleInvalidCompletionRate() {
        // given
        InvalidCompletionRateException exception = new InvalidCompletionRateException("완료율이 유효하지 않습니다");

        // when
        ResponseEntity<ApiResponse> response = exceptionHandler.handleInvalidCompletionRate(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("완료율이 유효하지 않습니다");
    }

    @Test
    @DisplayName("InvalidTokenException 처리")
    void handleInvalidToken() {
        // given
        InvalidTokenException exception = new InvalidTokenException("유효하지 않은 토큰입니다");

        // when
        ResponseEntity<ApiResponse> response = exceptionHandler.handleInvalidToken(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("유효하지 않은 토큰입니다");
    }
}
