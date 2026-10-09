package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakePasswordHasher;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.in.ChangePasswordCommand;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountCommand;
import com.cuymonitor.backend.domain.port.in.UpdateProfileCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountServiceTest {

    private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

    private InMemoryUserRepository users;
    private MutableClock clock;
    private UserAccountService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        clock = new MutableClock(CREATED.plus(Duration.ofDays(1)));
        service = new UserAccountService(users, new FakePasswordHasher(), clock);
        userId = saveUser(UserStatus.ACTIVE);
    }

    @Test
    void getCurrentUserReturnsTheAccount() {
        User user = service.getCurrentUser(userId);

        assertThat(user.getUsername()).isEqualTo("juan");
        assertThat(user.getEmail()).isEqualTo("juan@mail.com");
    }

    @Test
    void getCurrentUserFailsForUnknownId() {
        assertThatThrownBy(() -> service.getCurrentUser(UUID.randomUUID()))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void updateProfileChangesAndSavesTheName() {
        User updated = service.updateProfile(new UpdateProfileCommand(userId, "Juan Carlos"));

        assertThat(updated.getFullName()).isEqualTo("Juan Carlos");
        User stored = users.findById(userId).orElseThrow();
        assertThat(stored.getFullName()).isEqualTo("Juan Carlos");
        assertThat(stored.getUpdatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void changePasswordWithCorrectCurrentPassword() {
        service.changePassword(new ChangePasswordCommand(userId, "Secret-pass-1", "new-Secret-pass-1"));

        assertThat(users.findById(userId).orElseThrow().getPasswordHash()).isEqualTo("hashed:new-Secret-pass-1");
    }

    @Test
    void changePasswordWithWrongCurrentPasswordFails() {
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(userId, "wrong", "new-Secret-pass-1")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(users.findById(userId).orElseThrow().getPasswordHash()).isEqualTo("hashed:Secret-pass-1");
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(userId, "Secret-pass-1", "short")))
                .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void deactivateWithCorrectPasswordDisablesTheAccount() {
        service.deactivate(new DeactivateAccountCommand(userId, "Secret-pass-1"));

        User stored = users.findById(userId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(stored.canLogIn()).isFalse();
    }

    @Test
    void deactivateWithWrongPasswordFails() {
        assertThatThrownBy(() -> service.deactivate(new DeactivateAccountCommand(userId, "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(users.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void disabledAccountIsRejectedEverywhere() {
        UUID disabledId = saveUser(UserStatus.DISABLED);

        assertThatThrownBy(() -> service.getCurrentUser(disabledId))
                .isInstanceOf(AccountDisabledException.class);
        assertThatThrownBy(() -> service.updateProfile(new UpdateProfileCommand(disabledId, "Other")))
                .isInstanceOf(AccountDisabledException.class);
        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordCommand(disabledId, "Secret-pass-1", "new-Secret-pass-1")))
                .isInstanceOf(AccountDisabledException.class);
        assertThatThrownBy(() -> service.deactivate(new DeactivateAccountCommand(disabledId, "Secret-pass-1")))
                .isInstanceOf(AccountDisabledException.class);
    }

    private UUID saveUser(UserStatus status) {
        UUID id = UUID.randomUUID();
        String name = status == UserStatus.ACTIVE ? "juan" : "pedro";
        users.save(User.restore(id, name, "Juan", name + "@mail.com", "hashed:Secret-pass-1", status, CREATED, CREATED));
        return id;
    }
}
