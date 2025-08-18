package com.calendar.app.controller;

import com.calendar.app.dto.CommonResponse;
import com.calendar.app.dto.auth.RefreshTokenRequest;
import com.calendar.app.dto.auth.TokenDto;
import com.calendar.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "OAuth2 로그인 및 토큰 관리 API")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Google OAuth 로그인",
        description = "Google OAuth2 로그인을 시작합니다. 프론트엔드에서 이 URL을 호출하면 Google 로그인 페이지로 리다이렉트됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Google 로그인 페이지로 리다이렉트"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/login/google")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @Operation(
        summary = "서버 상태 확인",
        description = "서버가 정상적으로 동작하는지 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서버 정상 동작",
            content = @Content(examples = @ExampleObject(value = "{\"ok\": true}")))
    })
    @GetMapping("/status")
    public Map<String, Object> status() { 
        return Map.of("ok", true); 
    }

    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 형식")
    })
    @PostMapping("/refresh")
    public CommonResponse<?> refreshToken(
        @Parameter(description = "Refresh Token 정보", required = true)
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        TokenDto tokenDto = authService.refreshToken(request.getRefreshToken());
        return new CommonResponse<>(true, "토큰이 성공적으로 갱신되었습니다.", tokenDto);
    }
}
