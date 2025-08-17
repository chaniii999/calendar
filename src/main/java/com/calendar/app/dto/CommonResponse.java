package com.calendar.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "API 응답 공통 형식")
public class CommonResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public CommonResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
    }

    // 팩토리 메서드 추가
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, "OK", data);
    }

    public static <T> CommonResponse<T> fail(String message) {
        return new CommonResponse<>(false, message);
    }
}
