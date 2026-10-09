package com.cuymonitor.backend.adapter.out.security;

import com.cuymonitor.backend.domain.exception.GoogleSignInDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidGoogleTokenException;
import com.cuymonitor.backend.domain.model.auth.GoogleIdentity;
import com.cuymonitor.backend.domain.port.out.GoogleIdTokenVerifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

/**
 * Verifies a Google ID token. The decoder checks the signature against Google's public keys and the expiry;
 * this class checks who issued it and that it was made for OUR client id, so a token Google gave to some
 * other app cannot be replayed here.
 */
public class GoogleIdTokenVerifierAdapter implements GoogleIdTokenVerifier {

    // Google uses both spellings
    private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder decoder;
    private final String clientId;

    public GoogleIdTokenVerifierAdapter(JwtDecoder decoder, String clientId) {
        this.decoder = decoder;
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (clientId.isEmpty()) {
            throw new GoogleSignInDisabledException();
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new InvalidGoogleTokenException();
        }
        String issuer = jwt.getClaimAsString("iss");
        if (issuer == null || !ISSUERS.contains(issuer)
                || jwt.getAudience() == null || !jwt.getAudience().contains(clientId)
                || jwt.getSubject() == null || jwt.getClaimAsString("email") == null) {
            throw new InvalidGoogleTokenException();
        }
        return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"), emailVerified(jwt),
                jwt.getClaimAsString("name"));
    }

    // Google sends a boolean, some libraries stringify it
    private static boolean emailVerified(Jwt jwt) {
        Object value = jwt.getClaims().get("email_verified");
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }
}
