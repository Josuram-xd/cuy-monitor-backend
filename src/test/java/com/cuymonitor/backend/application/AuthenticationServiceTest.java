package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakePasswordHasher;
import com.cuymonitor.backend.application.fake.FakeTokenIssuer;
import com.cuymonitor.backend.application.fake.InMemoryOtpChallengeRepository;
import com.cuymonitor.backend.application.fake.InMemoryRefreshTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryRevokedTokenRepository;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.application.fake.RecordingOtpSender;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.InvalidOtpException;
import com.cuymonitor.backend.domain.exception.TooManyOtpRequestsException;
import com.cuymonitor.backend.domain.exception.UserAlreadyExistsException;
import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.LoginChallenge;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.in.LoginCommand;
import com.cuymonitor.backend.domain.port.in.RegisterUserCommand;
import com.cuymonitor.backend.domain.port.in.VerifyOtpCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS = 3;
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);

    private InMemoryUserRepository users;
    private InMemoryOtpChallengeRepository challenges;
    private RecordingOtpSender sender;
    private MutableClock clock;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        challenges = new InMemoryOtpChallengeRepository();
        sender = new RecordingOtpSender();
        clock = new MutableClock(NOW);
        SessionService sessions = new SessionService(new FakeTokenIssuer(), new InMemoryRefreshTokenRepository(),
                new InMemoryRevokedTokenRepository(), users, clock, Duration.ofDays(7), Duration.ofMinutes(15));
        service = new AuthenticationService(users, challenges, new FakePasswordHasher(), sender,
                sessions, clock, OTP_TTL, MAX_ATTEMPTS, MAX_REQUESTS, REQUEST_WINDOW);
    }

    @Test
    void registerCreatesPendingUserAndSendsCode() {
        LoginChallenge challenge = register("juan", "juan@mail.com");

        assertThat(challenge.expiresAt()).isEqualTo(NOW.plus(OTP_TTL));
        User saved = users.findByUsername("juan").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed:secret-pass");
        assertThat(sender.sent()).hasSize(1);
        assertThat(sender.sent().getFirst().email()).isEqualTo("juan@mail.com");
        assertThat(sender.lastCode()).matches("\\d{6}");
    }

    @Test
    void registerRejectsRepeatedUsername() {
        register("juan", "juan@mail.com");

        assertThatThrownBy(() -> register("JUAN", "other@mail.com")).isInstanceOf(UserAlreadyExistsException.class);
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void registerRejectsRepeatedEmail() {
        register("juan", "juan@mail.com");

        assertThatThrownBy(() -> register("pedro", "Juan@Mail.com")).isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("juan", "Juan", "juan@mail.com", "short")))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(sender.sent()).isEmpty();
    }

    @Test
    void loginWithCorrectPasswordSendsNewCode() {
        register("juan", "juan@mail.com");

        LoginChallenge challenge = service.login(new LoginCommand(" Juan ", "secret-pass"));

        assertThat(challenge.challengeId()).isNotNull();
        assertThat(sender.sent()).hasSize(2);
    }

    @Test
    void loginRevokesPreviousCodes() {
        LoginChallenge first = register("juan", "juan@mail.com");
        String firstCode = sender.lastCode();
        service.login(new LoginCommand("juan", "secret-pass"));

        assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(first.challengeId(), firstCode)))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void loginIsRejectedAfterTooManyCodesInTheWindow() {
        register("juan", "juan@mail.com");
        service.login(new LoginCommand("juan", "secret-pass"));
        service.login(new LoginCommand("juan", "secret-pass"));

        assertThatThrownBy(() -> service.login(new LoginCommand("juan", "secret-pass")))
                .isInstanceOf(TooManyOtpRequestsException.class);
        assertThat(sender.sent()).hasSize(3);
    }

    @Test
    void loginWorksAgainOnceTheWindowHasPassed() {
        register("juan", "juan@mail.com");
        service.login(new LoginCommand("juan", "secret-pass"));
        service.login(new LoginCommand("juan", "secret-pass"));
        clock.advance(REQUEST_WINDOW.plusSeconds(1));

        LoginChallenge challenge = service.login(new LoginCommand("juan", "secret-pass"));

        assertThat(challenge.challengeId()).isNotNull();
    }

    @Test
    void loginWithWrongPasswordFails() {
        register("juan", "juan@mail.com");

        assertThatThrownBy(() -> service.login(new LoginCommand("juan", "wrong-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithUnknownUserFailsWithSameError() {
        assertThatThrownBy(() -> service.login(new LoginCommand("ghost", "secret-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithDisabledAccountFails() {
        users.save(User.restore(UUID.randomUUID(), "juan", "Juan", "juan@mail.com", "hashed:secret-pass",
                UserStatus.DISABLED, NOW, NOW));

        assertThatThrownBy(() -> service.login(new LoginCommand("juan", "secret-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(sender.sent()).isEmpty();
    }

    @Test
    void correctCodeReturnsTokenAndActivatesAccount() {
        LoginChallenge challenge = register("juan", "juan@mail.com");

        AuthSession session = service.verify(new VerifyOtpCommand(challenge.challengeId(), sender.lastCode()));

        User user = users.findByUsername("juan").orElseThrow();
        assertThat(session.accessToken().accessToken()).isEqualTo("token-for-" + user.getId());
        assertThat(session.refreshToken()).isNotBlank();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void wrongCodeFailsAndCountsTheAttempt() {
        LoginChallenge challenge = register("juan", "juan@mail.com");

        assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(challenge.challengeId(), wrongCode())))
                .isInstanceOf(InvalidOtpException.class);
        assertThat(challenges.findById(challenge.challengeId()).orElseThrow().getAttempts()).isEqualTo(1);
    }

    @Test
    void expiredCodeFails() {
        LoginChallenge challenge = register("juan", "juan@mail.com");
        clock.advance(OTP_TTL);

        assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(challenge.challengeId(), sender.lastCode())))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void codeFailsAfterMaxAttempts() {
        LoginChallenge challenge = register("juan", "juan@mail.com");
        String code = sender.lastCode();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(challenge.challengeId(), wrongCode())))
                    .isInstanceOf(InvalidOtpException.class);
        }

        assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(challenge.challengeId(), code)))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void codeCannotBeReused() {
        LoginChallenge challenge = register("juan", "juan@mail.com");
        String code = sender.lastCode();
        service.verify(new VerifyOtpCommand(challenge.challengeId(), code));

        assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(challenge.challengeId(), code)))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void unknownChallengeFails() {
        assertThatThrownBy(() -> service.verify(new VerifyOtpCommand(UUID.randomUUID(), "123456")))
                .isInstanceOf(InvalidOtpException.class);
    }

    private LoginChallenge register(String username, String email) {
        return service.register(new RegisterUserCommand(username, "Juan Perez", email, "secret-pass"));
    }

    private String wrongCode() {
        return sender.lastCode().equals("000000") ? "111111" : "000000";
    }
}
