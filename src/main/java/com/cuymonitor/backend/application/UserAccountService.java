package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.UserNotFoundException;
import com.cuymonitor.backend.domain.model.user.PasswordPolicy;
import com.cuymonitor.backend.domain.model.user.ProfilePhoto;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.in.ChangePasswordCommand;
import com.cuymonitor.backend.domain.port.in.ChangePasswordUseCase;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountCommand;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountUseCase;
import com.cuymonitor.backend.domain.port.in.GetCurrentUserUseCase;
import com.cuymonitor.backend.domain.port.in.ProfilePhotoUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateProfileCommand;
import com.cuymonitor.backend.domain.port.in.UpdateProfileUseCase;
import com.cuymonitor.backend.domain.port.out.PasswordHasher;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class UserAccountService implements GetCurrentUserUseCase, UpdateProfileUseCase, ChangePasswordUseCase,
        DeactivateAccountUseCase, ProfilePhotoUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public UserAccountService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return loadActiveUser(userId);
    }

    @Override
    @Transactional
    public User updateProfile(UpdateProfileCommand command) {
        User user = loadActiveUser(command.userId());
        user.updateProfile(command.fullName(), clock.instant());
        user.changeNickname(command.nickname(), clock.instant());
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfilePhoto getPhoto(UUID userId) {
        return userRepository.findPhoto(userId)
                .orElseThrow(() -> new UserNotFoundException("this account has no photo"));
    }

    @Override
    @Transactional
    public void setPhoto(UUID userId, ProfilePhoto photo) {
        User user = loadActiveUser(userId);
        userRepository.savePhoto(userId, photo);
        // the account itself only remembers that it has one, so hasPhoto is right in the next answer
        user.attachPhoto(photo.contentType(), clock.instant());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removePhoto(UUID userId) {
        User user = loadActiveUser(userId);
        userRepository.deletePhoto(userId);
        user.attachPhoto(null, clock.instant());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = loadActiveUser(command.userId());
        checkCurrentPassword(user, command.currentPassword());
        PasswordPolicy.validate(command.newPassword(), user.getUsername(), user.getEmail());

        user.changePassword(passwordHasher.hashPassword(command.newPassword()), clock.instant());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivate(DeactivateAccountCommand command) {
        User user = loadActiveUser(command.userId());
        checkCurrentPassword(user, command.currentPassword());

        user.deactivate(clock.instant());
        userRepository.save(user);
    }

    // the JWT stays valid for up to 30 min after deactivation, so every request re-checks the account
    private User loadActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::canLogIn)
                .orElseThrow(AccountDisabledException::new);
    }

    private void checkCurrentPassword(User user, String currentPassword) {
        // an account made with Google has no password to type: its open session is the proof
        if (!user.hasPassword()) {
            return;
        }
        if (currentPassword == null || !passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
    }
}
