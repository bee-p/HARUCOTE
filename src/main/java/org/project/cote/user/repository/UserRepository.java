package org.project.cote.user.repository;

import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByNickname(String nickname);
}
