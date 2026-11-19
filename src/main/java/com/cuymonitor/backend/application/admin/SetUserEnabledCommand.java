package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Command — ConcreteCommand: switches an account off, or back on. */
public final class SetUserEnabledCommand implements AdminCommand<User> {

    private final UserRepository users;
    private final Clock clock;
    private final UUID adminId;
    private final UUID targetUserId;
    private final boolean enabled;

    public SetUserEnabledCommand(UserRepository users, Clock clock, UUID adminId, UUID targetUserId,
                                 boolean enabled) {
        this.users = Objects.requireNonNull(users, "users");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.adminId = adminId;
        this.targetUserId = targetUserId;
        this.enabled = enabled;
    }

    @Override
    public User execute() {
        User target = AdminUserRules.require(users, targetUserId);
        if (targetUserId.equals(adminId)) {
            throw new InvalidAdminActionException("you can't disable your own account from here");
        }
        if (!enabled && target.isAdmin()) {
            AdminUserRules.requireAnotherAdminRemains(users);
        }
        if (enabled) {
            target.reinstate(clock.instant());
        } else if (target.getStatus() != UserStatus.DISABLED) {
            target.deactivate(clock.instant());
        }
        return users.save(target);
    }

    @Override
    public String description() {
        return (enabled ? "enable account " : "disable account ") + targetUserId;
    }
}
