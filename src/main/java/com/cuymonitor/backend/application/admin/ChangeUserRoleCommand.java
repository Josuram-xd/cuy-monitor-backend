package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Command — ConcreteCommand: gives an account another role. */
public final class ChangeUserRoleCommand implements AdminCommand<User> {

    private final UserRepository users;
    private final Clock clock;
    private final UUID adminId;
    private final UUID targetUserId;
    private final UserRole role;

    public ChangeUserRoleCommand(UserRepository users, Clock clock, UUID adminId, UUID targetUserId,
                                 UserRole role) {
        this.users = Objects.requireNonNull(users, "users");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.adminId = adminId;
        this.targetUserId = targetUserId;
        this.role = Objects.requireNonNull(role, "role");
    }

    @Override
    public User execute() {
        User target = AdminUserRules.require(users, targetUserId);
        if (targetUserId.equals(adminId)) {
            throw new InvalidAdminActionException("you can't change your own role");
        }
        if (target.isAdmin() && role != UserRole.ADMIN) {
            AdminUserRules.requireAnotherAdminRemains(users);
        }
        target.changeRoleTo(role, clock.instant());
        return users.save(target);
    }

    @Override
    public String description() {
        return "change role of account " + targetUserId + " to " + role;
    }
}
