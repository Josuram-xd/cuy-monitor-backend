package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.InvalidOtpException;
import com.cuymonitor.backend.domain.exception.TooManyOtpRequestsException;
import com.cuymonitor.backend.domain.exception.UserAlreadyExistsException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.LoginChallenge;
import com.cuymonitor.backend.domain.model.auth.OtpChallenge;
import com.cuymonitor.backend.domain.model.auth.OtpVerificationResult;
import com.cuymonitor.backend.domain.model.user.PasswordPolicy;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.in.LoginCommand;
import com.cuymonitor.backend.domain.port.in.LoginUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterUserCommand;
import com.cuymonitor.backend.domain.port.in.RegisterUserUseCase;
import com.cuymonitor.backend.domain.port.in.VerifyOtpCommand;
import com.cuymonitor.backend.domain.port.in.VerifyOtpUseCase;
import com.cuymonitor.backend.domain.port.out.OtpChallengeRepository;
import com.cuymonitor.backend.domain.port.out.OtpSender;
import com.cuymonitor.backend.domain.port.out.PasswordHasher;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

public class AuthenticationService implements RegisterUserUseCase, LoginUseCase, VerifyOtpUseCase {

    private static final int CODE_BOUND = 1_000_000;

    private final UserRepository userRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordHasher passwordHasher;
    private final OtpSender otpSender;
    private final SessionService sessions;
    private final Clock clock;
    private final Duration otpTtl;
    private final int otpMaxAttempts;
    private final int otpMaxRequests;
    private final Duration otpRequestWindow;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthenticationService(UserRepository userRepository, OtpChallengeRepository otpChallengeRepository,
                                 PasswordHasher passwordHasher, OtpSender otpSender, SessionService sessions,
                                 Clock clock, Duration otpTtl, int otpMaxAttempts,
                                 int otpMaxRequests, Duration otpRequestWindow) {
        this.userRepository = userRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordHasher = passwordHasher;
        this.otpSender = otpSender;
        this.sessions = sessions;
        this.clock = clock;
        this.otpTtl = otpTtl;
        this.otpMaxAttempts = otpMaxAttempts;
        this.otpMaxRequests = otpMaxRequests;
        this.otpRequestWindow = otpRequestWindow;
        this.dummyHash = passwordHasher.hashPassword("dummy-password-for-timing");
    }

    @Override
    @Transactional
    public LoginChallenge register(RegisterUserCommand command) {
        PasswordPolicy.validate(command.password(), command.username(), command.email());
        Instant now = clock.instant();
        User user = User.register(command.username(), command.fullName(), command.email(),
                passwordHasher.hashPassword(command.password()), now);

        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException();
        }
        return issueChallenge(userRepository.save(user), now);
    }

    @Override
    @Transactional
    public LoginChallenge login(LoginCommand command) {
        if (command.username() == null || command.password() == null) {
            throw new InvalidCredentialsException();
        }
        Optional<User> found = userRepository.findByUsername(command.username().trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) {
            // hash anyway so the response time doesn't reveal whether the user exists
            passwordHasher.matches(command.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        if (!user.hasPassword()) {
            // an account that only uses Google: same answer and same cost as a wrong password
            passwordHasher.matches(command.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordHasher.matches(command.password(), user.getPasswordHash()) || !user.canLogIn()) {
            throw new InvalidCredentialsException();
        }
        return issueChallenge(user, clock.instant());
    }

    // no rollback on InvalidOtpException, otherwise the failed attempt would not be counted
    @Override
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public AuthSession verify(VerifyOtpCommand command) {
        if (command.challengeId() == null || command.code() == null) {
            throw new InvalidOtpException();
        }
        OtpChallenge challenge = otpChallengeRepository.findById(command.challengeId())
                .orElseThrow(InvalidOtpException::new);
        User user = userRepository.findById(challenge.getUserId())
                .filter(User::canLogIn)
                .orElseThrow(InvalidOtpException::new);

        Instant now = clock.instant();
        OtpVerificationResult result = challenge.verify(passwordHasher.matches(command.code(), challenge.getCodeHash()), now);
        otpChallengeRepository.save(challenge);
        if (result != OtpVerificationResult.VERIFIED) {
            throw new InvalidOtpException();
        }

        user.activate(now);
        return sessions.start(userRepository.save(user));
    }

    private LoginChallenge issueChallenge(User user, Instant now) {
        // caps how many emails one account can trigger (resend abuse)
        if (otpChallengeRepository.countIssuedSince(user.getId(), now.minus(otpRequestWindow)) >= otpMaxRequests) {
            throw new TooManyOtpRequestsException();
        }
        for (OtpChallenge pending : otpChallengeRepository.findPendingByUserId(user.getId())) {
            pending.revoke(now);
            otpChallengeRepository.save(pending);
        }

        String code = String.format("%06d", random.nextInt(CODE_BOUND));
        OtpChallenge challenge = otpChallengeRepository.save(
                OtpChallenge.issue(user.getId(), passwordHasher.hashPassword(code), now, otpTtl, otpMaxAttempts));
        otpSender.sendOtp(user.getEmail(), code, challenge.getExpiresAt());
        return new LoginChallenge(challenge.getId(), challenge.getExpiresAt());
    }
}
