package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.DemoSignInDisabledException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.in.DemoSignInUseCase;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * The demo account is named by configuration (app.demo.username). It must be an active breeder: a door with no
 * password must never open an admin account, so an admin named there turns the button off instead.
 */
public class DemoSignInService implements DemoSignInUseCase {

    private final UserRepository users;
    private final SessionService sessions;
    private final String demoUsername;

    public DemoSignInService(UserRepository users, SessionService sessions, String demoUsername) {
        this.users = Objects.requireNonNull(users, "users");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.demoUsername = demoUsername == null ? "" : demoUsername.trim();
    }

    @Override
    @Transactional
    public AuthSession signIn() {
        if (demoUsername.isEmpty()) {
            throw new DemoSignInDisabledException();
        }
        User demo = users.findByUsername(demoUsername)
                .filter(User::canLogIn)
                .filter(user -> !user.isAdmin())
                .orElseThrow(DemoSignInDisabledException::new);
        return sessions.start(demo);
    }
}
