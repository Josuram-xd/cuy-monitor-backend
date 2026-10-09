package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.InvalidRefreshTokenException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.auth.RefreshToken;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.in.LogoutCommand;
import com.cuymonitor.backend.domain.port.in.LogoutUseCase;
import com.cuymonitor.backend.domain.port.in.PurgeExpiredTokensUseCase;
import com.cuymonitor.backend.domain.port.in.RefreshSessionUseCase;
import com.cuymonitor.backend.domain.port.out.RefreshTokenRepository;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import com.cuymonitor.backend.domain.port.out.TokenIssuer;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Starts, renews and ends sessions. Every renewal rotates the refresh token: the old one dies and a new one
 * is born in the same family. Using a dead token again means somebody copied it, so the whole family is revoked.
 */
public class SessionService implements RefreshSessionUseCase, LogoutUseCase, PurgeExpiredTokensUseCase {

    private static final int TOKEN_BYTES = 32;
    // two tabs refreshing at the same moment is not theft: a token rotated this recently is just refused
    static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private final TokenIssuer tokenIssuer;
    private final RefreshTokenRepository refreshTokens;
    private final RevokedTokenRepository revokedTokens;
    private final UserRepository userRepository;
    private final Clock clock;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public SessionService(TokenIssuer tokenIssuer, RefreshTokenRepository refreshTokens,
                          RevokedTokenRepository revokedTokens, UserRepository userRepository, Clock clock,
                          Duration refreshTtl) {
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
        this.revokedTokens = revokedTokens;
        this.userRepository = userRepository;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    /** Called after the OTP is verified: a brand new family. */
    @Transactional
    public AuthSession start(User user) {
        return issueSession(user, UUID.randomUUID());
    }

    // no rollback on InvalidRefreshTokenException, otherwise revoking the family would be undone
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthSession refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Instant now = clock.instant();
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked()) {
            boolean justRotated = stored.getRevokedAt().orElseThrow().plus(REUSE_GRACE).isAfter(now);
            if (!justRotated) {
                refreshTokens.revokeFamily(stored.getFamilyId(), now);
            }
            throw new InvalidRefreshTokenException();
        }
        if (stored.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        User user = userRepository.findById(stored.getUserId())
                .filter(User::canLogIn)
                .orElseThrow(InvalidRefreshTokenException::new);

        stored.revoke(now);
        refreshTokens.save(stored);
        return issueSession(user, stored.getFamilyId());
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        Instant now = clock.instant();
        if (command.accessTokenId() != null && command.accessTokenExpiresAt() != null) {
            revokedTokens.revoke(command.accessTokenId(), command.accessTokenExpiresAt());
        }
        String raw = command.refreshToken();
        if (raw != null && !raw.isBlank()) {
            refreshTokens.findByTokenHash(hash(raw))
                    .ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), now));
        }
    }

    @Override
    @Transactional
    public void purgeExpired() {
        Instant now = clock.instant();
        revokedTokens.deleteExpired(now);
        refreshTokens.deleteExpired(now);
    }

    private AuthSession issueSession(User user, UUID familyId) {
        Instant now = clock.instant();
        AuthToken access = tokenIssuer.issueToken(user);
        String raw = newRawToken();
        RefreshToken refresh = refreshTokens.save(
                RefreshToken.issue(user.getId(), familyId, hash(raw), now, now.plus(refreshTtl)));
        return new AuthSession(access, raw, refresh.getExpiresAt());
    }

    private String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // the raw value is random (256 bits), so a plain SHA-256 is enough and lets us look it up by hash
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
