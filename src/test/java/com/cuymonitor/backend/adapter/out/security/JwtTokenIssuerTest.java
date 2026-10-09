package com.cuymonitor.backend.adapter.out.security;

import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenIssuerTest {

    private static final SecretKey KEY = key("test-secret-with-at-least-32-bytes!!");
    private static final Duration TTL = Duration.ofMinutes(30);

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)),
            "cuy-monitor-backend", TTL, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void issuedTokenIsValidAndHasTheExpectedClaims() {
        User user = user();

        AuthToken token = issuer.issueToken(user);
        Jwt jwt = decoder(KEY).decode(token.accessToken());

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("cuy-monitor-backend");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(TTL));
        assertThat(token.expiresAt()).isEqualTo(now.plus(TTL));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        AuthToken token = issuer.issueToken(user());

        assertThatThrownBy(() -> decoder(key("another-secret-with-at-least-32-bytes")).decode(token.accessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void issuedTokenCarriesAJtiThatIsAUuid() {
        Jwt jwt = decoder(KEY).decode(issuer.issueToken(user()).accessToken());

        assertThat(jwt.getId()).isNotBlank();
        assertThat(UUID.fromString(jwt.getId())).isNotNull();
    }

    @Test
    void everyTokenGetsItsOwnJti() {
        User user = user();

        Jwt first = decoder(KEY).decode(issuer.issueToken(user).accessToken());
        Jwt second = decoder(KEY).decode(issuer.issueToken(user).accessToken());

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    private static JwtDecoder decoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static User user() {
        Instant created = Instant.parse("2026-10-01T10:00:00Z");
        return User.restore(UUID.randomUUID(), "juan", "Juan", "juan@mail.com", "hash", UserStatus.ACTIVE,
                created, created);
    }
}
