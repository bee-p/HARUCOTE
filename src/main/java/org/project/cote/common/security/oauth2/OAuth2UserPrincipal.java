package org.project.cote.common.security.oauth2;

import org.project.cote.user.domain.User;
import org.project.cote.user.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 인증 성공 후 SecurityContext 에 들어갈 경량 principal.
 * 영속화된 User 엔티티 자체를 보유하면 의도치 않은 dirty checking / 직렬화 사고가 날 수 있어
 * 토큰 발급에 필요한 식별 필드만 추출해 둔다.
 */
public record OAuth2UserPrincipal(
        Long userId,
        UserRole role,
        String nickname,
        Map<String, Object> attributes
) implements OAuth2User {

    public static OAuth2UserPrincipal from(User user, Map<String, Object> attributes) {
        return new OAuth2UserPrincipal(user.getId(), user.getRole(), user.getNickname(), attributes);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
