package org.project.cote.common.security.oauth2;

import org.project.cote.user.domain.AuthProvider;

import java.util.Map;

public record GithubOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public AuthProvider provider() {
        return AuthProvider.GITHUB;
    }

    @Override
    public String providerId() {
        Object id = attributes.get("id");
        return id == null ? null : String.valueOf(id);
    }

    @Override
    public String email() {
        return (String) attributes.get("email");
    }

    @Override
    public String nickname() {
        return (String) attributes.get("login");
    }

    @Override
    public String profileImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}
