package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakeTokenIssuer;
import com.cuymonitor.backend.application.fake.InMemoryRefreshTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryRevokedTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.DemoSignInDisabledException;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoSignInServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final SessionService sessions = new SessionService(new FakeTokenIssuer(),
            new InMemoryRefreshTokenRepository(), new InMemoryRevokedTokenRepository(), users, new MutableClock(NOW),
            Duration.ofDays(7), Duration.ofMinutes(15));

    private void save(String username, UserRole role, UserStatus status) {
        // the demo account has the hash of a random password nobody knows: the button is the only way in
        users.save(User.restore(UUID.randomUUID(), username, "Demo", username + "@cuymonitor.local",
                "hash-of-a-password-nobody-knows", null, status, role, NOW, NOW));
    }

    @Test
    void opensASessionInTheDemoBreederAccount() {
        save("demo", UserRole.BREEDER, UserStatus.ACTIVE);

        assertThat(new DemoSignInService(users, sessions, "demo").signIn().refreshToken()).isNotBlank();
    }

    @Test
    void isOffWhenNoAccountIsConfigured() {
        assertThatThrownBy(() -> new DemoSignInService(users, sessions, " ").signIn())
                .isInstanceOf(DemoSignInDisabledException.class);
    }

    @Test
    void neverOpensAnAdminAccount() {
        save("boss", UserRole.ADMIN, UserStatus.ACTIVE);

        assertThatThrownBy(() -> new DemoSignInService(users, sessions, "boss").signIn())
                .isInstanceOf(DemoSignInDisabledException.class);
    }

    @Test
    void theAdminButtonOpensOnlyAnActiveAdmin() {
        save("boss", UserRole.ADMIN, UserStatus.ACTIVE);
        save("demo", UserRole.BREEDER, UserStatus.ACTIVE);

        assertThat(new DemoSignInService(users, sessions, "demo", "boss").signInAsAdmin().refreshToken())
                .isNotBlank();
        assertThatThrownBy(() -> new DemoSignInService(users, sessions, "demo", "demo").signInAsAdmin())
                .isInstanceOf(DemoSignInDisabledException.class);
        assertThatThrownBy(() -> new DemoSignInService(users, sessions, "demo", "").signInAsAdmin())
                .isInstanceOf(DemoSignInDisabledException.class);
    }

    @Test
    void isOffForAMissingOrDisabledAccount() {
        save("gone", UserRole.BREEDER, UserStatus.DISABLED);

        assertThatThrownBy(() -> new DemoSignInService(users, sessions, "gone").signIn())
                .isInstanceOf(DemoSignInDisabledException.class);
        assertThatThrownBy(() -> new DemoSignInService(users, sessions, "nobody").signIn())
                .isInstanceOf(DemoSignInDisabledException.class);
    }
}
