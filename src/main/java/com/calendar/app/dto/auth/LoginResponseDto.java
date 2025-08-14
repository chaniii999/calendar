package com.calendar.app.dto.auth;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String userId;
    private String email;
    private String nickname;
    private TokenDto token;
    private String message;
}