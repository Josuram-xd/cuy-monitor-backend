package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cuymonitor.backend.domain.model.user.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByGoogleSubject(String googleSubject);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // the admin screen: every account, newest first
    List<UserJpaEntity> findAllByOrderByCreatedAtDesc();

    long countByRole(UserRole role);
}
