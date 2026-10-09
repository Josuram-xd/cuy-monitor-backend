package com.cuymonitor.backend.adapter.out.security;

import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Rejects a token whose jti (this token) or sid (the whole session) was stored on logout, even if it has not
 * expired yet. A token without either claim was never issued by us.
 */
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "token was revoked", null);

    private final RevokedTokenRepository revokedTokens;

    public RevokedTokenValidator(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            UUID tokenId = UUID.fromString(jwt.getId());
            UUID sessionId = UUID.fromString(jwt.getClaimAsString(JwtTokenIssuer.SESSION_CLAIM));
            if (revokedTokens.isRevoked(tokenId) || revokedTokens.isRevoked(sessionId)) {
                return OAuth2TokenValidatorResult.failure(REVOKED);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // a missing or malformed jti / sid was never issued by us
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
