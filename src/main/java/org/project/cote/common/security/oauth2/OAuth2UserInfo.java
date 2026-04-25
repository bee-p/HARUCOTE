package org.project.cote.common.security.oauth2;

import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.user.domain.AuthProvider;

import java.util.Map;

/**
 * OAuth2 제공자별 attribute 차이를 흡수하는 추상화.
 */
public interface OAuth2UserInfo {

    AuthProvider provider();

    String providerId();

    String email();

    String nickname();

    String profileImageUrl();

    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        if ("github".equalsIgnoreCase(registrationId)) {
            return new GithubOAuth2UserInfo(attributes);
        }
        throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR,
                "지원하지 않는 OAuth2 공급자: " + registrationId);
    }
}
