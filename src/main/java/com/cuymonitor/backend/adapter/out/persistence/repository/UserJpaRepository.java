package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // The bytes are deliberately not a field of UserJpaEntity: an account is loaded on every request, and a
    // detached save() would write the whole row, blanking a photo nobody meant to touch. They are read and
    // written on their own, and always together with their type, which ck_app_user_photo requires.
    @Query(value = "SELECT photo FROM app_user WHERE id = :id", nativeQuery = true)
    byte[] findPhotoBytes(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE app_user SET photo = :photo, photo_content_type = :contentType WHERE id = :id",
            nativeQuery = true)
    void updatePhoto(@Param("id") UUID id, @Param("photo") byte[] photo,
                     @Param("contentType") String contentType);
}
