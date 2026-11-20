package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.DemoSignInDisabledException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.in.DemoSignInUseCase;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * The demo account is named by configuration (app.demo.username). It must be an active breeder: an admin named
 * there turns that button off. The admin button is a separate, explicit switch (app.demo.admin-username) that only
 * opens an active admin; empty keeps it off.
 */
public class DemoSignInService implements DemoSignInUseCase {

    private final UserRepository users;
    private final SessionService sessions;
    private final String demoUsername;
    private final String adminUsername;

    public DemoSignInService(UserRepository users, SessionService sessions, String demoUsername) {
        this(users, sessions, demoUsername, "");
    }

    public DemoSignInService(UserRepository users, SessionService sessions, String demoUsername,
                             String adminUsername) {
        this.users = Objects.requireNonNull(users, "users");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.demoUsername = demoUsername == null ? "" : demoUsername.trim();
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim();
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

    @Override
    @Transactional
    public AuthSession signInAsAdmin() {
        if (adminUsername.isEmpty()) {
            throw new DemoSignInDisabledException();
        }
        User admin = users.findByUsername(adminUsername)
                .filter(User::canLogIn)
                .filter(User::isAdmin)
                .orElseThrow(DemoSignInDisabledException::new);
        return sessions.start(admin);
    }
}
