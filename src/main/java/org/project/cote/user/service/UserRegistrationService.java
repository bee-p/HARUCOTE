package org.project.cote.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.common.security.oauth2.OAuth2UserInfo;
import org.project.cote.user.domain.User;
import org.project.cote.user.event.UserRegisteredEvent;
import org.project.cote.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * OAuth2 첫 로그인 시 자동 가입(JIT) 처리.
 * 외부 응답(GitHub 등)의 닉네임/이메일/이미지 URL을 안전하게 sanitize 한 뒤 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private static final int MAX_NICKNAME_RETRY = 5;
    private static final int NICKNAME_SUFFIX_MIN = 1000;
    private static final int NICKNAME_SUFFIX_MAX = 10000;
    private static final int NICKNAME_MAX_LENGTH = 30;
    private static final int EMAIL_MAX_LENGTH = 320;
    private static final int IMAGE_URL_MAX_LENGTH = 500;
    private static final Pattern NICKNAME_ALLOWED = Pattern.compile("^[\\p{L}\\p{N}_\\-]+$");
    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User upsertFromOAuth2(OAuth2UserInfo info) {
        validateUpstreamInfo(info);
        return userRepository.findByProviderAndProviderId(info.provider(), info.providerId())
                .orElseGet(() -> registerNew(info));
    }

    private void validateUpstreamInfo(OAuth2UserInfo info) {
        if (info.provider() == null) {
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR);
        }
        if (info.providerId() == null || info.providerId().isBlank()) {
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR);
        }
    }

    private User registerNew(OAuth2UserInfo info) {
        String nickname = resolveNickname(sanitizeNicknameHint(info.nickname()));
        String email = sanitizeEmail(info.email());
        String profileImageUrl = sanitizeImageUrl(info.profileImageUrl());

        User created;
        try {
            created = userRepository.save(
                    User.create(info.provider(), info.providerId(), email, nickname, profileImageUrl)
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("OAuth2 가입 시 동시성 충돌 (provider={}, providerId={})",
                    info.provider(), info.providerId(), e);
            throw new ApiException(ErrorCode.NICKNAME_ALREADY_TAKEN);
        }

        log.info("신규 사용자 가입: id={}, provider={}, nickname={}",
                created.getId(), created.getProvider(), created.getNickname());

        eventPublisher.publishEvent(
                new UserRegisteredEvent(created.getId(), created.getProvider(), created.getProviderId())
        );
        return created;
    }

    private String sanitizeNicknameHint(String hint) {
        if (hint == null) {
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR);
        }
        String trimmed = hint.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR);
        }
        if (!NICKNAME_ALLOWED.matcher(trimmed).matches()) {
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR);
        }
        if (trimmed.length() > NICKNAME_MAX_LENGTH) {
            return trimmed.substring(0, NICKNAME_MAX_LENGTH);
        }
        return trimmed;
    }

    private String sanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.length() > EMAIL_MAX_LENGTH) {
            return null;
        }
        if (!SIMPLE_EMAIL.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }

    private String sanitizeImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.length() > IMAGE_URL_MAX_LENGTH) {
            return null;
        }
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            return null;
        }
        return trimmed;
    }

    private String resolveNickname(String hint) {
        if (!userRepository.existsByNickname(hint)) {
            return hint;
        }
        for (int i = 0; i < MAX_NICKNAME_RETRY; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(NICKNAME_SUFFIX_MIN, NICKNAME_SUFFIX_MAX);
            String candidate = hint + "#" + suffix;
            if (candidate.length() <= NICKNAME_MAX_LENGTH + 5
                    && !userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ErrorCode.NICKNAME_ALREADY_TAKEN);
    }
}
