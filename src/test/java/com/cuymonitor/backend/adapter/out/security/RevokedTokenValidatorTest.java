package com.cuymonitor.backend.adapter.out.security;

import com.cuymonitor.backend.application.fake.InMemoryRevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RevokedTokenValidatorTest {

    private final InMemoryRevokedTokenRepository revoked = new InMemoryRevokedTokenRepository();
    private final RevokedTokenValidator validator = new RevokedTokenValidator(revoked);

    @Test
    void aTokenThatWasNotRevokedPasses() {
        assertThat(validator.validate(jwt(UUID.randomUUID().toString(), UUID.randomUUID().toString())).hasErrors())
                .isFalse();
    }

    @Test
    void aRevokedTokenFails() {
        UUID jti = UUID.randomUUID();
        revoked.revoke(jti, Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt(jti.toString(), UUID.randomUUID().toString())).hasErrors()).isTrue();
    }

    @Test
    void everyTokenOfARevokedSessionFails() {
        UUID sid = UUID.randomUUID();
        revoked.revoke(sid, Instant.now().plusSeconds(600));

        // two different tokens (jti) of the same login
        assertThat(validator.validate(jwt(UUID.randomUUID().toString(), sid.toString())).hasErrors()).isTrue();
        assertThat(validator.validate(jwt(UUID.randomUUID().toString(), sid.toString())).hasErrors()).isTrue();
    }

    @Test
    void aTokenWithoutJtiOrSidFails() {
        assertThat(validator.validate(jwt(null, UUID.randomUUID().toString())).hasErrors()).isTrue();
        assertThat(validator.validate(jwt(UUID.randomUUID().toString(), null)).hasErrors()).isTrue();
    }

    @Test
    void aJtiOrSidThatIsNotAUuidFails() {
        assertThat(validator.validate(jwt("not-a-uuid", UUID.randomUUID().toString())).hasErrors()).isTrue();
        assertThat(validator.validate(jwt(UUID.randomUUID().toString(), "not-a-uuid")).hasErrors()).isTrue();
    }

    private static Jwt jwt(String jti, String sid) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject("user")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600));
        if (jti != null) {
            builder.jti(jti);
        }
        if (sid != null) {
            builder.claim("sid", sid);
        }
        return builder.build();
    }
}
