package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.ChangePasswordRequest;
import com.cuymonitor.backend.adapter.in.web.dto.DeactivateAccountRequest;
import com.cuymonitor.backend.adapter.in.web.dto.UpdateProfileRequest;
import com.cuymonitor.backend.adapter.in.web.dto.UserResponse;
import com.cuymonitor.backend.domain.port.in.ChangePasswordCommand;
import com.cuymonitor.backend.domain.port.in.ChangePasswordUseCase;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountCommand;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountUseCase;
import com.cuymonitor.backend.domain.port.in.GetCurrentUserUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateProfileCommand;
import com.cuymonitor.backend.domain.port.in.UpdateProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeactivateAccountUseCase deactivateAccountUseCase;

    public UserAccountController(GetCurrentUserUseCase getCurrentUserUseCase,
                                 UpdateProfileUseCase updateProfileUseCase,
                                 ChangePasswordUseCase changePasswordUseCase,
                                 DeactivateAccountUseCase deactivateAccountUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.deactivateAccountUseCase = deactivateAccountUseCase;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getCurrentUserUseCase.getCurrentUser(userId(jwt)));
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateProfileUseCase.updateProfile(
                new UpdateProfileCommand(userId(jwt), request.fullName())));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordUseCase.changePassword(
                new ChangePasswordCommand(userId(jwt), request.currentPassword(), request.newPassword()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeactivateAccountRequest request) {
        deactivateAccountUseCase.deactivate(new DeactivateAccountCommand(userId(jwt), request.currentPassword()));
    }

    // the id always comes from the token, never from the URL, so nobody can touch another account
    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
