package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakePasswordHasher;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import com.cuymonitor.backend.domain.exception.UserNotFoundException;
import com.cuymonitor.backend.domain.model.ImageRules;
import com.cuymonitor.backend.domain.model.user.ProfilePhoto;
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
        User updated = service.updateProfile(new UpdateProfileCommand(userId, "Juan Carlos", null));

        assertThat(updated.getFullName()).isEqualTo("Juan Carlos");
        User stored = users.findById(userId).orElseThrow();
        assertThat(stored.getFullName()).isEqualTo("Juan Carlos");
        assertThat(stored.getUpdatedAt()).isEqualTo(clock.instant());
    }

    // --- the nickname: a short name the breeder picks, with the full name as the fallback ---

    @Test
    void anAccountWithoutANicknameIsShownByItsFullName() {
        assertThat(service.getCurrentUser(userId).displayName()).isEqualTo("Juan");
    }

    @Test
    void savesTheNicknameAndShowsItInsteadOfTheFullName() {
        User updated = service.updateProfile(new UpdateProfileCommand(userId, "Juan Perez", "Juanito"));

        assertThat(updated.getNickname()).isEqualTo("Juanito");
        assertThat(updated.displayName()).isEqualTo("Juanito");
        assertThat(users.findById(userId).orElseThrow().getNickname()).isEqualTo("Juanito");
    }

    @Test
    void clearingTheNicknameGoesBackToTheFullName() {
        service.updateProfile(new UpdateProfileCommand(userId, "Juan Perez", "Juanito"));

        User cleared = service.updateProfile(new UpdateProfileCommand(userId, "Juan Perez", "   "));

        assertThat(cleared.getNickname()).isNull();
        assertThat(cleared.displayName()).isEqualTo("Juan Perez");
    }

    @Test
    void refusesANicknameLongerThanTheColumn() {
        String tooLong = "a".repeat(User.MAX_NICKNAME_LENGTH + 1);

        assertThatThrownBy(() -> service.updateProfile(new UpdateProfileCommand(userId, "Juan", tooLong)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- the photo ---

    @Test
    void anAccountStartsWithoutAPhoto() {
        assertThat(service.getCurrentUser(userId).hasPhoto()).isFalse();
        assertThatThrownBy(() -> service.getPhoto(userId)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void savesThePhotoAndHandsItBack() {
        var photo = new ProfilePhoto(new byte[] {1, 2, 3}, "image/webp");

        service.setPhoto(userId, photo);

        assertThat(service.getPhoto(userId)).isEqualTo(photo);
        // the account remembers it has one, so the dashboard knows to ask for it
        assertThat(service.getCurrentUser(userId).hasPhoto()).isTrue();
    }

    @Test
    void replacingThePhotoKeepsOnlyTheNewOne() {
        service.setPhoto(userId, new ProfilePhoto(new byte[] {1}, "image/png"));
        var second = new ProfilePhoto(new byte[] {9, 9}, "image/jpeg");

        service.setPhoto(userId, second);

        assertThat(service.getPhoto(userId)).isEqualTo(second);
    }

    @Test
    void removingThePhotoLeavesTheAccountWithout() {
        service.setPhoto(userId, new ProfilePhoto(new byte[] {1}, "image/png"));

        service.removePhoto(userId);

        assertThat(service.getCurrentUser(userId).hasPhoto()).isFalse();
        assertThatThrownBy(() -> service.getPhoto(userId)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void removingAPhotoNobodySetIsNotAnError() {
        service.removePhoto(userId);

        assertThat(service.getCurrentUser(userId).hasPhoto()).isFalse();
    }

    @Test
    void changingTheNameDoesNotLoseThePhoto() {
        var photo = new ProfilePhoto(new byte[] {1, 2, 3}, "image/webp");
        service.setPhoto(userId, photo);

        service.updateProfile(new UpdateProfileCommand(userId, "Otro nombre", "Juanito"));

        // the bytes are written on their own, so an ordinary save of the account cannot blank them
        assertThat(service.getPhoto(userId)).isEqualTo(photo);
        assertThat(service.getCurrentUser(userId).hasPhoto()).isTrue();
    }

    @Test
    void refusesAPictureBiggerThanTheColumnAllows() {
        byte[] tooBig = new byte[ImageRules.MAX_BYTES + 1];

        assertThatThrownBy(() -> new ProfilePhoto(tooBig, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(ImageRules.MAX_BYTES));
    }

    @Test
    void refusesATypeTheBrowserCouldBeTrickedWith() {
        assertThatThrownBy(() -> new ProfilePhoto(new byte[] {1}, "image/svg+xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePasswordWithCorrectCurrentPassword() {
        service.changePassword(new ChangePasswordCommand(userId, "Secret-pass-1", "new-Secret-pass-1"));

        assertThat(users.findById(userId).orElseThrow().getPasswordHash()).isEqualTo("hashed:new-Secret-pass-1");
    }

    @Test
    void anAccountMadeWithGoogleCanSetItsFirstPasswordWithoutAnOldOne() {
        UUID googleUser = users.save(User.registerWithGoogle("ana", "Ana", "ana@gmail.com", "sub-1", CREATED)).getId();

        service.changePassword(new ChangePasswordCommand(googleUser, null, "Brand-new-pass-9"));

        User stored = users.findById(googleUser).orElseThrow();
        assertThat(stored.hasPassword()).isTrue();
        assertThat(stored.getPasswordHash()).isEqualTo("hashed:Brand-new-pass-9");
    }

    @Test
    void thePasswordPolicyAlsoAppliesToTheFirstPasswordOfAGoogleAccount() {
        UUID googleUser = users.save(User.registerWithGoogle("ana", "Ana", "ana@gmail.com", "sub-1", CREATED)).getId();

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(googleUser, null, "weak")))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(users.findById(googleUser).orElseThrow().hasPassword()).isFalse();
    }

    @Test
    void anAccountMadeWithGoogleCanBeDeactivatedWithoutAPassword() {
        UUID googleUser = users.save(User.registerWithGoogle("ana", "Ana", "ana@gmail.com", "sub-1", CREATED)).getId();

        service.deactivate(new DeactivateAccountCommand(googleUser, null));

        assertThat(users.findById(googleUser).orElseThrow().getStatus()).isEqualTo(UserStatus.DISABLED);
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
        assertThatThrownBy(() -> service.updateProfile(new UpdateProfileCommand(disabledId, "Other", null)))
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
