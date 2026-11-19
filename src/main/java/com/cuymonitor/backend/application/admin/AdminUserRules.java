package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.exception.UserNotFoundException;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.util.Objects;
import java.util.UUID;

/** The rules both account commands share. */
final class AdminUserRules {

    private AdminUserRules() {
    }

    static User require(UserRepository users, UUID userId) {
        return users.findById(Objects.requireNonNull(userId, "userId"))
                .orElseThrow(() -> new UserNotFoundException("there is no account with id " + userId));
    }

    /**
     * A safety net, not a rule anybody meets through the API: to demote or disable the only admin you would
     * have to be an admin, and the only admin is yourself, which the commands already refuse. It stays because
     * a command can run without the proxy (a job, a test), and losing the last admin would leave the catalog
     * uneditable with no way back through the application.
     */
    static void requireAnotherAdminRemains(UserRepository users) {
        if (users.countAdmins() <= 1) {
            throw new InvalidAdminActionException(
                    "this is the last admin: name another one first, or nobody could edit the catalog again");
        }
    }
}
