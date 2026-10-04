package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> users = new HashMap<>();

    @Override
    public User save(User user) {
        users.put(user.getId(), copy(user));
        return copy(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id)).map(InMemoryUserRepository::copy);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst()
                .map(InMemoryUserRepository::copy);
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.values().stream().anyMatch(u -> u.getUsername().equals(username));
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream().anyMatch(u -> u.getEmail().equals(email));
    }

    public int count() {
        return users.size();
    }

    // stored as copies so a test fails if the service forgets to call save()
    private static User copy(User u) {
        return User.restore(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPasswordHash(),
                u.getStatus(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
