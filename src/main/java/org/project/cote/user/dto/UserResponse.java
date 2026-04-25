package org.project.cote.user.dto;

import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        AuthProvider provider,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getProvider(),
                user.getCreatedAt()
        );
    }
}
