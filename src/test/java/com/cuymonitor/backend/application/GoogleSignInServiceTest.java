package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakeTokenIssuer;
import com.cuymonitor.backend.application.fake.InMemoryRefreshTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryRevokedTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidGoogleTokenException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.GoogleIdentity;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleSignInServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

    private InMemoryUserRepository users;
    private GoogleSignInService service;
    private GoogleIdentity identity;
    private boolean tokenIsValid;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        MutableClock clock = new MutableClock(NOW);
        SessionService sessions = new SessionService(new FakeTokenIssuer(), new InMemoryRefreshTokenRepository(),
                new InMemoryRevokedTokenRepository(), users, clock, Duration.ofDays(7), Duration.ofMinutes(15));
        identity = new GoogleIdentity("sub-1", "Ana.Ruiz@Gmail.com", true, "Ana Ruiz");
        tokenIsValid = true;
        service = new GoogleSignInService(users, token -> {
            if (!tokenIsValid) {
                throw new InvalidGoogleTokenException();
            }
            return identity;
        }, sessions, clock);
    }

    @Test
    void theFirstVisitCreatesAnActiveAccountWithoutPasswordAndStartsASession() {
        AuthSession session = service.signIn("token");

        User created = users.findByGoogleSubject("sub-1").orElseThrow();
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.hasPassword()).isFalse();
        assertThat(created.getEmail()).isEqualTo("ana.ruiz@gmail.com");
        assertThat(created.getUsername()).isEqualTo("ana.ruiz");
        assertThat(created.getFullName()).isEqualTo("Ana Ruiz");
        assertThat(session.accessToken().accessToken()).isEqualTo("token-for-" + created.getId());
        assertThat(session.refreshToken()).isNotBlank();
    }

    @Test
    void theSecondVisitUsesTheSameAccount() {
        service.signIn("token");
        service.signIn("token");

        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void aTakenUsernameGetsANumberAppended() {
        users.save(User.register("ana.ruiz", "Other Ana", "other@mail.com", "hash", NOW));

        service.signIn("token");

        String username = users.findByGoogleSubject("sub-1").orElseThrow().getUsername();
        assertThat(username).startsWith("ana.ruiz").isNotEqualTo("ana.ruiz").hasSize("ana.ruiz".length() + 4);
    }

    @Test
    void aStrangeOrShortEmailNameStillGivesAValidUsername() {
        identity = new GoogleIdentity("sub-2", "a@gmail.com", true, null);

        service.signIn("token");

        User created = users.findByGoogleSubject("sub-2").orElseThrow();
        assertThat(created.getUsername()).isEqualTo("user");
        assertThat(created.getFullName()).isEqualTo("a");
    }

    @Test
    void anExistingVerifiedAccountWithTheSameEmailIsLinkedAndKeepsItsPassword() {
        User existing = users.save(User.restore(UUID.randomUUID(), "ana", "Ana", "ana.ruiz@gmail.com", "hash",
                UserStatus.ACTIVE, NOW.minusSeconds(3600), NOW.minusSeconds(3600)));

        service.signIn("token");

        User linked = users.findById(existing.getId()).orElseThrow();
        assertThat(linked.getGoogleSubject()).isEqualTo("sub-1");
        assertThat(linked.hasPassword()).isTrue();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void anUnverifiedAccountWithTheSameEmailLosesItsPasswordWhenTheOwnerArrivesThroughGoogle() {
        // somebody registered the owner's email first, with a password only they know
        users.save(User.register("squatter", "Squatter", "ana.ruiz@gmail.com", "squatter-hash", NOW.minusSeconds(60)));

        service.signIn("token");

        User taken = users.findByGoogleSubject("sub-1").orElseThrow();
        assertThat(taken.getUsername()).isEqualTo("squatter");
        assertThat(taken.hasPassword()).isFalse();
        assertThat(taken.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void aGoogleAccountWhoseEmailIsNotVerifiedIsRefused() {
        identity = new GoogleIdentity("sub-1", "ana.ruiz@gmail.com", false, "Ana");

        assertThatThrownBy(() -> service.signIn("token")).isInstanceOf(InvalidGoogleTokenException.class);
        assertThat(users.count()).isZero();
    }

    @Test
    void anInvalidTokenCreatesNothing() {
        tokenIsValid = false;

        assertThatThrownBy(() -> service.signIn("forged")).isInstanceOf(InvalidGoogleTokenException.class);
        assertThat(users.count()).isZero();
    }

    @Test
    void aDisabledAccountCannotComeBackThroughGoogle() {
        users.save(User.restore(UUID.randomUUID(), "ana", "Ana", "ana.ruiz@gmail.com", "hash",
                UserStatus.DISABLED, NOW.minusSeconds(3600), NOW.minusSeconds(60)));

        assertThatThrownBy(() -> service.signIn("token")).isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void aDisabledGoogleAccountIsRefusedToo() {
        users.save(User.restore(UUID.randomUUID(), "ana", "Ana", "ana.ruiz@gmail.com", null, "sub-1",
                UserStatus.DISABLED, NOW.minusSeconds(3600), NOW.minusSeconds(60)));

        assertThatThrownBy(() -> service.signIn("token")).isInstanceOf(AccountDisabledException.class);
    }
}
