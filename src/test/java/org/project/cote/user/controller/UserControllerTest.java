package org.project.cote.user.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.common.security.AuthenticatedUser;
import org.project.cote.common.security.jwt.JwtTokenProvider;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.UserRole;
import org.project.cote.user.dto.UserResponse;
import org.project.cote.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filters 비활성화 + SecurityContextHolder 직접 설정 방식.
 * {@code with(authentication(...))} 는 SecurityContextHolderFilter 가 있어야 동작하므로,
 * filter chain 을 끄는 슬라이스 테스트에서는 SecurityContextHolder 를 직접 채워준다.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.SliceConfig.class)
class UserControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpAuthentication() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/users/me 는 인증된 사용자의 정보를 반환한다")
    void getMe_returnsUserInfo() throws Exception {
        UserResponse response = new UserResponse(
                USER_ID, "u@e.com", "octocat", "https://img",
                AuthProvider.GITHUB, Instant.parse("2025-01-01T00:00:00Z")
        );
        when(userService.getMe(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.nickname").value("octocat"))
                .andExpect(jsonPath("$.data.email").value("u@e.com"))
                .andExpect(jsonPath("$.data.provider").value("GITHUB"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/nickname 은 닉네임을 변경한 후 사용자 정보를 반환한다")
    void changeNickname_returnsUpdatedInfo() throws Exception {
        UserResponse response = new UserResponse(
                USER_ID, null, "hubot", null,
                AuthProvider.GITHUB, Instant.parse("2025-01-01T00:00:00Z")
        );
        when(userService.changeNickname(eq(USER_ID), eq("hubot"))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"hubot\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("hubot"));
    }

    @Test
    @DisplayName("닉네임 변경 시 빈 값이면 400 INVALID_REQUEST 로 응답한다")
    void changeNickname_emptyValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("닉네임 충돌 시 409 NICKNAME_ALREADY_TAKEN 로 응답한다")
    void changeNickname_alreadyTaken_returns409() throws Exception {
        when(userService.changeNickname(any(), any()))
                .thenThrow(new ApiException(ErrorCode.NICKNAME_ALREADY_TAKEN));

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"hubot\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NICKNAME_ALREADY_TAKEN"));
    }

    @TestConfiguration
    static class SliceConfig {
        @Bean
        org.project.cote.common.exception.GlobalExceptionHandler globalExceptionHandler() {
            return new org.project.cote.common.exception.GlobalExceptionHandler();
        }
    }
}
