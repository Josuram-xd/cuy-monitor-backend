package com.cuymonitor.backend.adapter.out.security;

import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Rejects a token whose jti was stored on logout, even if it has not expired yet. */
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "token was revoked", null);

    private final RevokedTokenRepository revokedTokens;

    public RevokedTokenValidator(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getId() == null) {
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }
        try {
            if (revokedTokens.isRevoked(UUID.fromString(jwt.getId()))) {
                return OAuth2TokenValidatorResult.failure(REVOKED);
            }
        } catch (IllegalArgumentException e) {
            // a jti that is not a UUID was never issued by us
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
