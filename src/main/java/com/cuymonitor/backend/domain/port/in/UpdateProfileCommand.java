package com.cuymonitor.backend.domain.port.in;

import java.util.UUID;

/** nickname is optional and null clears it; the screens then fall back to the full name. */
public record UpdateProfileCommand (UUID userId, String fullName, String nickname) {
}
