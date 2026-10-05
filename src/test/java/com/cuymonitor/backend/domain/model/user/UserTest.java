package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void registerCreatesPendingUserWithNormalizedUsernameAndEmail() {
        User user = User.register("  Juan.Perez ", " Juan Perez ", "Juan@Mail.COM", "hash", NOW);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("juan.perez");
        assertThat(user.getFullName()).isEqualTo("Juan Perez");
        assertThat(user.getEmail()).isEqualTo("juan@mail.com");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void registerRejectsBlankFields() {
        assertThatThrownBy(() -> User.register(" ", "Juan", "juan@mail.com", "hash", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.register("juan", "Juan", "juan@mail.com", "", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activateMovesPendingUserToActive() {
        User user = User.register("juan", "Juan", "juan@mail.com", "hash", NOW);
        Instant later = NOW.plusSeconds(60);

        user.activate(later);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void activateDoesNothingWhenAlreadyActive() {
        User user = restore(UserStatus.ACTIVE);

        user.activate(NOW.plusSeconds(60));

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void activateFailsForDisabledUser() {
        User user = restore(UserStatus.DISABLED);

        assertThatThrownBy(() -> user.activate(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onlyDisabledUsersCannotLogIn() {
        assertThat(restore(UserStatus.PENDING_VERIFICATION).canLogIn()).isTrue();
        assertThat(restore(UserStatus.ACTIVE).canLogIn()).isTrue();
        assertThat(restore(UserStatus.DISABLED).canLogIn()).isFalse();
    }

    @Test
    void updateProfileChangesFullName() {
        User user = restore(UserStatus.ACTIVE);
        Instant later = NOW.plusSeconds(60);

        user.updateProfile("  Juan Carlos Perez ", later);

        assertThat(user.getFullName()).isEqualTo("Juan Carlos Perez");
        assertThat(user.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void updateProfileRejectsBlankName() {
        User user = restore(UserStatus.ACTIVE);

        assertThatThrownBy(() -> user.updateProfile(" ", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getFullName()).isEqualTo("Juan");
    }

    @Test
    void changePasswordReplacesTheHash() {
        User user = restore(UserStatus.ACTIVE);
        Instant later = NOW.plusSeconds(60);

        user.changePassword("new-hash", later);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void deactivateDisablesTheAccount() {
        User user = restore(UserStatus.ACTIVE);
        Instant later = NOW.plusSeconds(60);

        user.deactivate(later);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.canLogIn()).isFalse();
        assertThat(user.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void disabledAccountCannotBeChanged() {
        User user = restore(UserStatus.DISABLED);

        assertThatThrownBy(() -> user.updateProfile("Other", NOW)).isInstanceOf(AccountDisabledException.class);
        assertThatThrownBy(() -> user.changePassword("new-hash", NOW)).isInstanceOf(AccountDisabledException.class);
        assertThatThrownBy(() -> user.deactivate(NOW)).isInstanceOf(AccountDisabledException.class);
        assertThat(user.getFullName()).isEqualTo("Juan");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
    }

    private static User restore(UserStatus status) {
        return User.restore(UUID.randomUUID(), "juan", "Juan", "juan@mail.com", "hash", status, NOW, NOW);
    }
}
