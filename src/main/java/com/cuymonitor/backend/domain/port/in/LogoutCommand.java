package com.cuymonitor.backend.domain.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * What the web adapter could read from the request cookies. Every field may be null:
 * logging out must work even when the access token already expired.
 */
public record LogoutCommand(UUID accessTokenId, Instant accessTokenExpiresAt, String refreshToken) {
}
