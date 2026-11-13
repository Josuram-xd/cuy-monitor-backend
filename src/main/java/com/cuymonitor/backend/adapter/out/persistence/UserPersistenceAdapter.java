package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.UserJpaRepository;
import com.cuymonitor.backend.domain.model.user.ProfilePhoto;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository, UserPersistenceMapper mapper) {
        this.userJpaRepository = userJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        return mapper.toDomain(userJpaRepository.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAdmins() {
        return userJpaRepository.countByRole(UserRole.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfilePhoto> findPhoto(UUID userId) {
        return userJpaRepository.findById(userId)
                .filter(entity -> entity.getPhotoContentType() != null)
                .map(entity -> new ProfilePhoto(userJpaRepository.findPhotoBytes(userId),
                        entity.getPhotoContentType()));
    }

    @Override
    @Transactional
    public void savePhoto(UUID userId, ProfilePhoto photo) {
        userJpaRepository.updatePhoto(userId, photo.content(), photo.contentType());
    }

    @Override
    @Transactional
    public void deletePhoto(UUID userId) {
        userJpaRepository.updatePhoto(userId, null, null);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return userJpaRepository.findByGoogleSubject(googleSubject).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }
}
