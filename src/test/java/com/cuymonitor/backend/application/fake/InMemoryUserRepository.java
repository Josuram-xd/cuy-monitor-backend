package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    public List<User> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public long countAdmins() {
        return users.values().stream().filter(User::isAdmin).count();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst()
                .map(InMemoryUserRepository::copy);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream().filter(u -> u.getEmail().equals(email)).findFirst()
                .map(InMemoryUserRepository::copy);
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return users.values().stream().filter(u -> googleSubject.equals(u.getGoogleSubject())).findFirst()
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
                u.getGoogleSubject(), u.getStatus(), u.getRole(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
