package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
