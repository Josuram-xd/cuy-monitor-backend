package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.user.User;

public interface UpdateProfileUseCase {
    User updateProfile(UpdateProfileCommand updateProfileCommand);
}
