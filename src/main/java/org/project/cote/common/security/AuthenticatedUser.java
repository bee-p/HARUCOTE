package org.project.cote.common.security;

import org.project.cote.user.domain.UserRole;

public record AuthenticatedUser(Long userId, UserRole role) {
}
