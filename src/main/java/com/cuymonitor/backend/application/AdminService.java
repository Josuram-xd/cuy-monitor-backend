package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.domain.port.out.AdminReportRepository;
import java.util.Map;
import com.cuymonitor.backend.domain.access.AdminOperations;
import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.exception.UserNotFoundException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The real subject behind {@link com.cuymonitor.backend.domain.access.AdminAccessProxy}. It assumes the caller
 * is already allowed: checking that is the proxy's job, and nothing else wires this class to a controller.
 *
 * <p>What it does guard are the rules that hold no matter who is asking — an admin cannot change their own role
 * or switch themselves off, and the last admin cannot be removed, which would leave the catalog uneditable with
 * no way back through the app.
 */
public class AdminService implements AdminOperations {

    public static final int MAX_AI_LOG = 200;

    private final CatalogOptionRepository catalog;
    private final UserRepository users;
    private final Clock clock;
    private final AdminReportRepository reports;
    private final SettingsService settings;

    public AdminService(CatalogOptionRepository catalog, UserRepository users, Clock clock,
                        AdminReportRepository reports, SettingsService settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.users = Objects.requireNonNull(users, "users");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.reports = Objects.requireNonNull(reports, "reports");
    }

    @Override
    public Map<UUID, AccountActivity> accountActivity(UUID requesterId) {
        return reports.activityByAccount();
    }

    @Override
    public List<SettingView> settings(UUID requesterId) {
        return settings.view();
    }

    @Override
    public List<SettingView> updateSettings(UUID requesterId, Map<String, String> changes) {
        try {
            return settings.update(changes, requesterId);
        } catch (IllegalArgumentException e) {
            throw new InvalidAdminActionException(e.getMessage());
        }
    }

    @Override
    public List<SettingView> resetSettings(UUID requesterId) {
        return settings.reset(requesterId);
    }

    @Override
    public List<AiLogEntry> aiLog(UUID requesterId, int limit) {
        if (limit < 1 || limit > MAX_AI_LOG) {
            throw new InvalidAdminActionException("the log shows between 1 and " + MAX_AI_LOG + " lines");
        }
        return reports.latestAiLog(limit);
    }

    @Override
    public List<CatalogOption> listCatalog(UUID requesterId) {
        return catalog.findAll();
    }

    @Override
    public CatalogOption saveCatalogOption(UUID requesterId, CatalogOption option) {
        Objects.requireNonNull(option, "option");
        if (option.code().length() > CatalogOption.MAX_CODE_LENGTH) {
            throw new InvalidAdminActionException(
                    "a code can have at most " + CatalogOption.MAX_CODE_LENGTH + " characters");
        }
        // the database only allows A-Z, 0-9 and _ (ck_catalog_option_code): say so here instead of failing on insert
        if (!option.code().matches("[A-Z0-9_]+")) {
            throw new InvalidAdminActionException("a code can only have letters, numbers and _");
        }
        if (option.hex() != null && !option.hex().matches("#[0-9a-f]{6}")) {
            throw new InvalidAdminActionException("a color must look like #a1b2c3, in lower case");
        }
        return catalog.save(option);
    }

    @Override
    public CatalogOption setCatalogOptionActive(UUID requesterId, CatalogKind kind, String code, boolean active) {
        String normalized = CatalogOption.normalize(code);
        CatalogOption option = catalog.find(kind, normalized)
                .orElseThrow(() -> new InvalidAdminActionException("there is no " + kind + " called " + normalized));
        return catalog.save(option.withActive(active));
    }

    @Override
    public List<User> listUsers(UUID requesterId) {
        return users.findAll();
    }

    @Override
    public User changeUserRole(UUID requesterId, UUID targetUserId, UserRole role) {
        Objects.requireNonNull(role, "role");
        User target = require(targetUserId);
        if (targetUserId.equals(requesterId)) {
            throw new InvalidAdminActionException("you can't change your own role");
        }
        if (target.isAdmin() && role != UserRole.ADMIN) {
            requireAnotherAdminRemains();
        }
        target.changeRoleTo(role, clock.instant());
        return users.save(target);
    }

    @Override
    public User setUserEnabled(UUID requesterId, UUID targetUserId, boolean enabled) {
        User target = require(targetUserId);
        if (targetUserId.equals(requesterId)) {
            throw new InvalidAdminActionException("you can't disable your own account from here");
        }
        if (!enabled && target.isAdmin()) {
            requireAnotherAdminRemains();
        }
        if (enabled) {
            target.reinstate(clock.instant());
        } else if (target.getStatus() != UserStatus.DISABLED) {
            target.deactivate(clock.instant());
        }
        return users.save(target);
    }

    /**
     * A safety net, not a rule anybody meets through the API: to demote or disable the only admin you would
     * have to be an admin, and the only admin is yourself, which the checks above already refuse. It stays
     * because this service is callable without the proxy — a job, a command, a use case added later — and
     * losing the last admin would leave the catalog uneditable with no way back through the application.
     */
    private void requireAnotherAdminRemains() {
        if (users.countAdmins() <= 1) {
            throw new InvalidAdminActionException(
                    "this is the last admin: name another one first, or nobody could edit the catalog again");
        }
    }

    private User require(UUID userId) {
        return users.findById(Objects.requireNonNull(userId, "userId"))
                .orElseThrow(() -> new UserNotFoundException("there is no account with id " + userId));
    }
}
