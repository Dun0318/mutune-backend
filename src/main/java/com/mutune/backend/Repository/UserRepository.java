package com.mutune.backend.Repository;

import com.mutune.backend.Entity.AuthProvider;
import com.mutune.backend.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
  DB 접근 전담
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);
    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
    Optional<UserEntity> findByRefreshToken(String refreshToken);
}
