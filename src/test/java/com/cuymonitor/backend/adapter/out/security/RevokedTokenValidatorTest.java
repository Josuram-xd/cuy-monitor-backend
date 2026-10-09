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
        assertThat(validator.validate(jwt(UUID.randomUUID().toString())).hasErrors()).isFalse();
    }

    @Test
    void aRevokedTokenFails() {
        UUID jti = UUID.randomUUID();
        revoked.revoke(jti, Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt(jti.toString())).hasErrors()).isTrue();
    }

    @Test
    void aTokenWithoutJtiFails() {
        assertThat(validator.validate(jwt(null)).hasErrors()).isTrue();
    }

    @Test
    void aJtiThatIsNotAUuidFails() {
        assertThat(validator.validate(jwt("not-a-uuid")).hasErrors()).isTrue();
    }

    private static Jwt jwt(String jti) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject("user")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600));
        if (jti != null) {
            builder.jti(jti);
        }
        return builder.build();
    }
}
