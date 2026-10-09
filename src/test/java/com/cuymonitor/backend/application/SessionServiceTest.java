package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakeTokenIssuer;
import com.cuymonitor.backend.application.fake.InMemoryRefreshTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryRevokedTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.InvalidRefreshTokenException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.RefreshToken;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.in.LogoutCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenRepository refreshTokens;
    private InMemoryRevokedTokenRepository revokedTokens;
    private MutableClock clock;
    private SessionService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        revokedTokens = new InMemoryRevokedTokenRepository();
        clock = new MutableClock(NOW);
        service = new SessionService(new FakeTokenIssuer(), refreshTokens, revokedTokens, users, clock, REFRESH_TTL, ACCESS_TTL);
        user = users.save(User.restore(UUID.randomUUID(), "juan", "Juan", "juan@mail.com", "hash",
                UserStatus.ACTIVE, NOW, NOW));
    }

    @Test
    void startIssuesAnAccessTokenAndAHashedRefreshToken() {
        AuthSession session = service.start(user);

        assertThat(session.accessToken().accessToken()).isEqualTo("token-for-" + user.getId());
        assertThat(session.refreshToken()).hasSizeGreaterThanOrEqualTo(40);
        assertThat(session.refreshExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        RefreshToken stored = refreshTokens.findByTokenHash(SessionService.hash(session.refreshToken())).orElseThrow();
        assertThat(stored.getTokenHash()).isNotEqualTo(session.refreshToken());
        assertThat(stored.getUserId()).isEqualTo(user.getId());
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    void everyLoginGetsItsOwnRefreshToken() {
        assertThat(service.start(user).refreshToken()).isNotEqualTo(service.start(user).refreshToken());
    }

    @Test
    void refreshRotatesTheTokenInsideTheSameFamily() {
        AuthSession first = service.start(user);

        AuthSession second = service.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        RefreshToken old = refreshTokens.findByTokenHash(SessionService.hash(first.refreshToken())).orElseThrow();
        RefreshToken fresh = refreshTokens.findByTokenHash(SessionService.hash(second.refreshToken())).orElseThrow();
        assertThat(old.isRevoked()).isTrue();
        assertThat(fresh.isRevoked()).isFalse();
        assertThat(fresh.getFamilyId()).isEqualTo(old.getFamilyId());
    }

    @Test
    void reusingARotatedTokenAfterTheGraceRevokesTheWholeFamily() {
        AuthSession first = service.start(user);
        AuthSession second = service.refresh(first.refreshToken());
        clock.advance(SessionService.REUSE_GRACE.plusSeconds(1));

        assertThatThrownBy(() -> service.refresh(first.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);

        // the thief and the real user both lose the session
        assertThatThrownBy(() -> service.refresh(second.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void reusingARotatedTokenRightAwayIsRefusedButKeepsTheFamilyAlive() {
        AuthSession first = service.start(user);
        AuthSession second = service.refresh(first.refreshToken());
        clock.advance(Duration.ofSeconds(2));

        assertThatThrownBy(() -> service.refresh(first.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(service.refresh(second.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    void unknownBlankOrMissingTokensAreRejected() {
        assertThatThrownBy(() -> service.refresh("never-issued")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.refresh(" ")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void anExpiredRefreshTokenIsRejected() {
        AuthSession session = service.start(user);
        clock.advance(REFRESH_TTL.plusSeconds(1));

        assertThatThrownBy(() -> service.refresh(session.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void aDisabledAccountCannotRefresh() {
        AuthSession session = service.start(user);
        users.save(User.restore(user.getId(), "juan", "Juan", "juan@mail.com", "hash", UserStatus.DISABLED, NOW, NOW));

        assertThatThrownBy(() -> service.refresh(session.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesTheAccessTokenAndEndsTheRefreshFamily() {
        AuthSession session = service.start(user);
        UUID jti = UUID.randomUUID();

        UUID sessionId = refreshTokens.findByTokenHash(SessionService.hash(session.refreshToken()))
                .orElseThrow().getFamilyId();

        service.logout(new LogoutCommand(jti, sessionId, NOW.plusSeconds(900), session.refreshToken()));

        assertThat(revokedTokens.isRevoked(jti)).isTrue();
        // every access token of this login dies too, not only the one that was in the cookie
        assertThat(revokedTokens.isRevoked(sessionId)).isTrue();
        assertThatThrownBy(() -> service.refresh(session.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void theSessionStaysRevokedAsLongAsAnAccessTokenCouldLive() {
        AuthSession session = service.start(user);
        UUID sessionId = refreshTokens.findByTokenHash(SessionService.hash(session.refreshToken()))
                .orElseThrow().getFamilyId();
        service.logout(new LogoutCommand(null, sessionId, null, session.refreshToken()));

        clock.advance(ACCESS_TTL.plusSeconds(1));
        service.purgeExpired();

        // past the access lifetime nothing issued for that session can be valid anymore
        assertThat(revokedTokens.isRevoked(sessionId)).isFalse();
    }

    @Test
    void reuseOfARotatedTokenKillsTheAccessTokensOfTheSessionToo() {
        AuthSession first = service.start(user);
        UUID sessionId = refreshTokens.findByTokenHash(SessionService.hash(first.refreshToken()))
                .orElseThrow().getFamilyId();
        service.refresh(first.refreshToken());
        clock.advance(SessionService.REUSE_GRACE.plusSeconds(1));

        assertThatThrownBy(() -> service.refresh(first.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(revokedTokens.isRevoked(sessionId)).isTrue();
    }

    @Test
    void logoutWorksWithOnlyTheRefreshToken() {
        AuthSession session = service.start(user);

        service.logout(new LogoutCommand(null, null, null, session.refreshToken()));

        assertThatThrownBy(() -> service.refresh(session.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutWithNothingToRevokeDoesNotFail() {
        assertThatCode(() -> service.logout(new LogoutCommand(null, null, null, null))).doesNotThrowAnyException();
        assertThatCode(() -> service.logout(new LogoutCommand(null, null, null, "unknown"))).doesNotThrowAnyException();
    }

    @Test
    void purgeDropsOnlyWhatAlreadyExpired() {
        UUID oldJti = UUID.randomUUID();
        UUID freshJti = UUID.randomUUID();
        revokedTokens.revoke(oldJti, NOW.plusSeconds(60));
        revokedTokens.revoke(freshJti, NOW.plus(Duration.ofHours(1)));
        service.start(user);
        clock.advance(Duration.ofMinutes(5));

        service.purgeExpired();

        assertThat(revokedTokens.isRevoked(oldJti)).isFalse();
        assertThat(revokedTokens.isRevoked(freshJti)).isTrue();
        assertThat(refreshTokens.count()).isEqualTo(1);

        clock.advance(REFRESH_TTL);
        service.purgeExpired();
        assertThat(refreshTokens.count()).isZero();
    }
}
