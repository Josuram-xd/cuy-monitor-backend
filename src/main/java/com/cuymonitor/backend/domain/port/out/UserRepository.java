package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.user.ProfilePhoto;
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

    /**
     * The picture's bytes. They are kept out of {@link User} on purpose: an account is loaded on every single
     * request, and 300 KB has no business travelling with it.
     */
    Optional<ProfilePhoto> findPhoto(UUID userId);

    /** Writes the bytes and their type together, which is what the database's CHECK demands. */
    void savePhoto(UUID userId, ProfilePhoto photo);

    void deletePhoto(UUID userId);
}
