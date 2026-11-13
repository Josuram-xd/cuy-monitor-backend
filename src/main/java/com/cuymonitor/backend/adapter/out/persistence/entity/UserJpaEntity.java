package com.cuymonitor.backend.adapter.out.persistence.entity;

import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserJpaEntity {
    @Id
    private UUID id;

    @Column(name= "username", nullable = false, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "nickname", length = 30)
    private String nickname;

    // the bytes live in the same row but are only read when the picture is asked for: see photo()
    @Column(name = "photo_content_type", length = 30)
    private String photoContentType;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    // null for an account that only signs in with Google
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {}

    public UserJpaEntity(UUID id, String username, String fullName, String email, String passwordHash,
                         String googleSubject, UserStatus status, UserRole role, String nickname,
                         String photoContentType, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
        this.status = status;
        this.role = role;
        this.nickname = nickname;
        this.photoContentType = photoContentType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public UserRole getRole() {
        return role;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public String getEmail() {
        return email;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
