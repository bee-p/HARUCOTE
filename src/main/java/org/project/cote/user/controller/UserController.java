package org.project.cote.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.cote.common.dto.ApiResponse;
import org.project.cote.common.security.AuthenticatedUser;
import org.project.cote.user.dto.NicknameChangeRequest;
import org.project.cote.user.dto.UserResponse;
import org.project.cote.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        UserResponse response = userService.getMe(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> changeNickname(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NicknameChangeRequest request
    ) {
        UserResponse response = userService.changeNickname(principal.userId(), request.nickname());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
