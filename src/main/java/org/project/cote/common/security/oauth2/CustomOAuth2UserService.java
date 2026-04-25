package org.project.cote.common.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.project.cote.user.domain.User;
import org.project.cote.user.service.UserRegistrationService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * OAuth2 제공자에서 사용자 정보를 받아 자체 User 엔티티로 매핑한다 (JIT 가입).
 * 결과는 SuccessHandler 가 사용할 {@link OAuth2UserPrincipal} 로 래핑.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRegistrationService userRegistrationService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User upstream = loadFromProvider(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo info = OAuth2UserInfo.of(registrationId, upstream.getAttributes());
        User user = userRegistrationService.upsertFromOAuth2(info);
        return OAuth2UserPrincipal.from(user, upstream.getAttributes());
    }

    /** Test seam — 서브클래스가 외부 호출을 stub 할 수 있도록 분리. */
    protected OAuth2User loadFromProvider(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }
}
