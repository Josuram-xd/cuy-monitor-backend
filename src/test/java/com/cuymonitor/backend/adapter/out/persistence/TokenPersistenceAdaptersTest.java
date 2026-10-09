package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.RefreshTokenPersistenceMapper;
import com.cuymonitor.backend.domain.model.auth.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the real Postgres with the cuy-monitor-db schema (same as contextLoads), so Hibernate
 * validates the entities against V5 and V6. Each test rolls back, nothing is left in the database.
 */
@DataJpaTest(properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RevokedTokenPersistenceAdapter.class, RefreshTokenPersistenceAdapter.class, RefreshTokenPersistenceMapper.class})
class TokenPersistenceAdaptersTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private RevokedTokenPersistenceAdapter revokedTokens;
    @Autowired
    private RefreshTokenPersistenceAdapter refreshTokens;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID userId;

    @BeforeEach
    void createUser() {
        userId = UUID.randomUUID();
        jdbc.update("insert into app_user (id, username, full_name, email, password_hash, status, created_at, updated_at) "
                        + "values (?, ?, 'Test', ?, 'hash', 'ACTIVE', now(), now())",
                userId, "u" + userId.toString().substring(0, 8), userId + "@mail.com");
    }

    @Test
    void aRevokedTokenIsFoundUntilItIsPurged() {
        UUID jti = UUID.randomUUID();
        assertThat(revokedTokens.isRevoked(jti)).isFalse();

        revokedTokens.revoke(jti, NOW.plusSeconds(600));
        revokedTokens.revoke(jti, NOW.plusSeconds(600));

        assertThat(revokedTokens.isRevoked(jti)).isTrue();
        revokedTokens.deleteExpired(NOW.plusSeconds(601));
        assertThat(revokedTokens.isRevoked(jti)).isFalse();
    }

    @Test
    void aRefreshTokenIsSavedAndFoundByItsHash() {
        RefreshToken token = RefreshToken.issue(userId, UUID.randomUUID(), "a".repeat(64), NOW, NOW.plusSeconds(600));

        refreshTokens.save(token);

        RefreshToken found = refreshTokens.findByTokenHash("a".repeat(64)).orElseThrow();
        assertThat(found.getId()).isEqualTo(token.getId());
        assertThat(found.getFamilyId()).isEqualTo(token.getFamilyId());
        assertThat(found.getExpiresAt()).isEqualTo(token.getExpiresAt());
        assertThat(found.isRevoked()).isFalse();
        assertThat(refreshTokens.findByTokenHash("b".repeat(64))).isEmpty();
    }

    @Test
    void revokingAFamilyRevokesEveryTokenInIt() {
        UUID family = UUID.randomUUID();
        refreshTokens.save(RefreshToken.issue(userId, family, "c".repeat(64), NOW, NOW.plusSeconds(600)));
        refreshTokens.save(RefreshToken.issue(userId, family, "d".repeat(64), NOW, NOW.plusSeconds(600)));
        refreshTokens.save(RefreshToken.issue(userId, UUID.randomUUID(), "e".repeat(64), NOW, NOW.plusSeconds(600)));

        refreshTokens.revokeFamily(family, NOW.plusSeconds(5));

        assertThat(refreshTokens.findByTokenHash("c".repeat(64)).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokens.findByTokenHash("d".repeat(64)).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokens.findByTokenHash("e".repeat(64)).orElseThrow().isRevoked()).isFalse();
    }

    @Test
    void expiredRefreshTokensArePurged() {
        refreshTokens.save(RefreshToken.issue(userId, UUID.randomUUID(), "f".repeat(64), NOW, NOW.plusSeconds(60)));
        refreshTokens.save(RefreshToken.issue(userId, UUID.randomUUID(), "9".repeat(64), NOW, NOW.plusSeconds(3600)));

        refreshTokens.deleteExpired(NOW.plusSeconds(120));

        assertThat(refreshTokens.findByTokenHash("f".repeat(64))).isEmpty();
        assertThat(refreshTokens.findByTokenHash("9".repeat(64))).isPresent();
    }
}
