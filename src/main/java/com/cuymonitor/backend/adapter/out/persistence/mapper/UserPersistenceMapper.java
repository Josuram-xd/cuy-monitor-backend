package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.UserJpaEntity;
import com.cuymonitor.backend.domain.model.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPasswordHash(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public User toDomain(UserJpaEntity entity) {
        return User.restore(entity.getId(), entity.getUsername(), entity.getFullName(), entity.getEmail(),
                entity.getPasswordHash(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
