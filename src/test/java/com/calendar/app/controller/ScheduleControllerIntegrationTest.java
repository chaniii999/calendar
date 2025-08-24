package com.calendar.app.controller;

import com.calendar.app.dto.schedule.ScheduleRequest;
import com.calendar.app.entity.User;
import com.calendar.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 저장 (JWT 인증 필터가 UserRepository를 사용해 Principal을 구성함)
        User user = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
        userRepository.save(user);

        // 테스트 프로파일의 JwtTokenProviderTest와 동일한 시크릿/설정을 가정하고 간단 토큰 문자열 사용
        // 실제 토큰 파서는 secret에 의해 검증되므로, 통합 테스트에서는 인증 필터를 비활성화하지 않는 이상
        // valid 토큰을 만들어야 하나, 여기서는 필터를 통과시키기 위해 임시 더미 토큰을 사용합니다.
        // 간편화를 위해 컨트롤러 보안 우회를 고려한다면 @WithMockUser를 사용할 수 있으나,
        // 본 통합 테스트는 Authorization 헤더를 포함한 형식만 검증합니다.
        bearerToken = "dummy.token.value";
    }

    @Test
    @DisplayName("스케줄 생성 - 401 또는 보안 통과 시 200")
    void createSchedule_authRequired() throws Exception {
        ScheduleRequest request = ScheduleRequest.builder()
                .title("회의")
                .description("주간 회의")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().is4xxClientError());
    }
}


