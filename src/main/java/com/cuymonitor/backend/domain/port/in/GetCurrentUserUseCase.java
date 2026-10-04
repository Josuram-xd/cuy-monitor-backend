package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.user.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {
    User getCurrentUser(UUID userId);
}
