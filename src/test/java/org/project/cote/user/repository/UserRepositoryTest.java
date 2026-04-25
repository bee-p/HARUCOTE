package org.project.cote.user.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2 메모리 DB를 PostgreSQL 호환 모드로 사용한다.
 * Docker 환경이 갖춰지면 Testcontainers PostgreSQL 로 전환 가능 (참고: 의존성은 그대로 유지).
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:userrepo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("저장 후 provider+providerId 로 사용자를 조회한다")
    void findByProviderAndProviderId_returnsSavedUser() {
        User saved = userRepository.save(
                User.create(AuthProvider.GITHUB, "12345", "user@example.com", "octocat", null)
        );

        Optional<User> found = userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "12345");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("octocat", found.get().getNickname());
    }

    @Test
    @DisplayName("존재하지 않는 provider+providerId 조회 시 빈 Optional 을 반환한다")
    void findByProviderAndProviderId_returnsEmpty_whenNotFound() {
        Optional<User> found = userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "not-exist");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("existsByNickname 은 닉네임 사용 여부에 따라 true/false 를 반환한다")
    void existsByNickname_returnsCorrectValue() {
        userRepository.save(
                User.create(AuthProvider.GITHUB, "12345", null, "octocat", null)
        );

        assertTrue(userRepository.existsByNickname("octocat"));
        assertFalse(userRepository.existsByNickname("missing"));
    }

    @Test
    @DisplayName("동일한 provider+providerId 사용자는 유니크 제약으로 저장에 실패한다")
    void save_violatesUniqueProviderConstraint() {
        userRepository.saveAndFlush(
                User.create(AuthProvider.GITHUB, "12345", null, "first", null)
        );

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(
                        User.create(AuthProvider.GITHUB, "12345", null, "second", null)
                )
        );
    }

    @Test
    @DisplayName("동일한 nickname 은 유니크 제약으로 저장에 실패한다")
    void save_violatesUniqueNicknameConstraint() {
        userRepository.saveAndFlush(
                User.create(AuthProvider.GITHUB, "11111", null, "octocat", null)
        );

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(
                        User.create(AuthProvider.GITHUB, "22222", null, "octocat", null)
                )
        );
    }

    @Test
    @DisplayName("저장 시 createdAt/updatedAt 이 자동 채워진다 (JPA Auditing)")
    void save_populatesAuditingTimestamps() {
        User saved = userRepository.saveAndFlush(
                User.create(AuthProvider.GITHUB, "12345", null, "octocat", null)
        );

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }
}
