package com.cuymonitor.backend.domain.port.in;

import java.util.UUID;

public record ChangePasswordCommand (UUID userId, String currentPassword, String newPassword) {

    @Override
    public String toString() {
        return "userId: " + userId + ", currentPassword: ***, newPassword: ***";
    }
}
