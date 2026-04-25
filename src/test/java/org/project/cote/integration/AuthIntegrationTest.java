package org.project.cote.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.security.jwt.JwtTokenProvider;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.project.cote.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityFilterChain + JwtAuthenticationFilter + JwtAuthenticationEntryPoint 까지 통과시켜
 * 인증 헤더 처리 → 401 매핑 → 200 정상 응답을 검증하는 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("유효 Bearer 토큰으로 /api/v1/users/me 요청 시 200 + 본인 정보 반환")
    void me_withValidToken_returns200() throws Exception {
        User saved = userRepository.save(
                User.create(AuthProvider.GITHUB, "12345", "u@e.com", "octocat", null)
        );
        String token = jwtTokenProvider.createAccessToken(saved.getId(), saved.getRole());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.nickname").value("octocat"));
    }

    @Test
    @DisplayName("토큰 없이 /api/v1/users/me 요청 시 401 AUTH_UNAUTHORIZED")
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("위조된 토큰으로 /api/v1/users/me 요청 시 401 AUTH_INVALID_TOKEN")
    void me_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    @DisplayName("public 경로(/api/v1/problems/**) 는 토큰 없이도 401 이 아닌 정상 라우팅")
    void publicPath_withoutToken_isNot401() throws Exception {
        // problems/random 은 외부 LeetCode 호출이라 200 보장 어려움. 401 이 아닌 것만 확인.
        int status = mockMvc.perform(get("/api/v1/problems/random"))
                .andReturn().getResponse().getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, status,
                "public 경로는 401 이 아니어야 함 (실제 status=" + status + ")");
    }

    @Test
    @DisplayName("인증된 사용자가 닉네임을 변경하면 200 + 갱신된 정보를 반환한다")
    void changeNickname_authenticated_returnsUpdated() throws Exception {
        User saved = userRepository.save(
                User.create(AuthProvider.GITHUB, "12345", null, "octocat", null)
        );
        String token = jwtTokenProvider.createAccessToken(saved.getId(), saved.getRole());

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nickname\":\"hubot\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("hubot"));

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("hubot", reloaded.getNickname());
    }
}
