package org.project.cote.user.event;

import org.project.cote.user.domain.AuthProvider;

/**
 * 신규 사용자 가입 시 발행되는 도메인 이벤트.
 * Phase 5 (Pet 도메인) 에서 기본 펫 지급 등의 후속 작업이 구독한다.
 * 리스너는 {@code @TransactionalEventListener(AFTER_COMMIT)} 사용 권장.
 */
public record UserRegisteredEvent(Long userId, AuthProvider provider, String providerId) {
}
