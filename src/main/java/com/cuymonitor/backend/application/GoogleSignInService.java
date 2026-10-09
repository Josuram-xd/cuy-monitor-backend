package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidGoogleTokenException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.GoogleIdentity;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.in.GoogleSignInUseCase;
import com.cuymonitor.backend.domain.port.out.GoogleIdTokenVerifier;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Sign in or sign up with a Google ID token. Google already proved the email, so there is no code to type:
 * a verified token starts the session directly.
 * <ul>
 *   <li>Known Google id: that account.</li>
 *   <li>Unknown Google id but a known email: the account is linked (an unverified one loses its password,
 *       see {@link User#linkGoogle}).</li>
 *   <li>Nothing known: a new active account without password.</li>
 * </ul>
 */
public class GoogleSignInService implements GoogleSignInUseCase {

    private static final int MAX_USERNAME_BASE = 40;
    private static final int MIN_USERNAME_BASE = 3;
    private static final int SUFFIX_TRIES = 20;

    private final UserRepository userRepository;
    private final GoogleIdTokenVerifier verifier;
    private final SessionService sessions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public GoogleSignInService(UserRepository userRepository, GoogleIdTokenVerifier verifier,
                               SessionService sessions, Clock clock) {
        this.userRepository = userRepository;
        this.verifier = verifier;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthSession signIn(String idToken) {
        GoogleIdentity identity = verifier.verify(idToken);
        // an unverified email proves nothing: it could be anybody's
        if (!identity.emailVerified()) {
            throw new InvalidGoogleTokenException();
        }
        Instant now = clock.instant();
        User user = userRepository.findByGoogleSubject(identity.subject())
                .orElseGet(() -> linkOrCreate(identity, now));
        if (!user.canLogIn()) {
            throw new AccountDisabledException();
        }
        return sessions.start(user);
    }

    private User linkOrCreate(GoogleIdentity identity, Instant now) {
        String email = identity.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.canLogIn()) {
                throw new AccountDisabledException();
            }
            user.linkGoogle(identity.subject(), now);
            return userRepository.save(user);
        }
        String name = identity.name() == null || identity.name().isBlank() ? email.split("@")[0] : identity.name();
        return userRepository.save(User.registerWithGoogle(freeUsername(email), name, email, identity.subject(), now));
    }

    // "ana.ruiz@gmail.com" -> "ana.ruiz"; if it is taken, "ana.ruiz4821"
    private String freeUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-z0-9._-]", "");
        if (base.length() < MIN_USERNAME_BASE) {
            base = "user";
        }
        if (base.length() > MAX_USERNAME_BASE) {
            base = base.substring(0, MAX_USERNAME_BASE);
        }
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        for (int i = 0; i < SUFFIX_TRIES; i++) {
            String candidate = base + (1000 + random.nextInt(9000));
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        return base + System.nanoTime();
    }
}
