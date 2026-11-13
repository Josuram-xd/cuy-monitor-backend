package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Every account, newest first. Only the admin screen needs this. */
    List<User> findAll();

    /** So the last admin can't be demoted or disabled, locking everybody out of the catalog. */
    long countAdmins();
}
