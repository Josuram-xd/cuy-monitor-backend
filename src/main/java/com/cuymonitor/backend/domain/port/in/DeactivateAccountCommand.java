package com.cuymonitor.backend.domain.port.in;

import java.util.UUID;

public record DeactivateAccountCommand (UUID userId, String currentPassword) {

    @Override
    public String toString() {
        return "userId: " + userId + ", currentPassword: ***";
    }
}
