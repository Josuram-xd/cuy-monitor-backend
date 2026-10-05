package com.cuymonitor.backend.domain.port.in;

import java.util.UUID;

public record UpdateProfileCommand (UUID userId, String fullName) {
}
