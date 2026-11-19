package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.admin.AdminCommandInvoker;
import com.cuymonitor.backend.application.admin.ChangeUserRoleCommand;
import com.cuymonitor.backend.application.admin.ResetSettingsCommand;
import com.cuymonitor.backend.application.admin.SaveCatalogOptionCommand;
import com.cuymonitor.backend.application.admin.SetCatalogOptionActiveCommand;
import com.cuymonitor.backend.application.admin.SetUserEnabledCommand;
import com.cuymonitor.backend.application.admin.UpdateSettingsCommand;
import com.cuymonitor.backend.domain.access.AdminOperations;
import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AdminAction;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.port.out.AdminReportRepository;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The real subject behind {@link com.cuymonitor.backend.domain.access.AdminAccessProxy}. It assumes the caller
 * is already allowed: checking that is the proxy's job, and nothing else wires this class to a controller.
 *
 * <p>It is also the <b>Client of the Command pattern</b>: every change (catalog, accounts, settings) is built as
 * a command object from {@code application.admin} and handed to the {@link AdminCommandInvoker}, which runs it
 * and writes down who did it. The reads (lists, reports) are not changes, so they go straight to the ports.
 */
public class AdminService implements AdminOperations {

    public static final int MAX_AI_LOG = 200;

    private final CatalogOptionRepository catalog;
    private final UserRepository users;
    private final Clock clock;
    private final AdminReportRepository reports;
    private final SettingsService settings;
    private final AdminCommandInvoker invoker;

    public AdminService(CatalogOptionRepository catalog, UserRepository users, Clock clock,
                        AdminReportRepository reports, SettingsService settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.users = Objects.requireNonNull(users, "users");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.reports = Objects.requireNonNull(reports, "reports");
        this.invoker = new AdminCommandInvoker(clock);
    }

    // ---- reads: no command, nothing to undo or remember

    @Override
    public Map<UUID, AccountActivity> accountActivity(UUID requesterId) {
        return reports.activityByAccount();
    }

    @Override
    public List<SettingView> settings(UUID requesterId) {
        return settings.view();
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
    public List<User> listUsers(UUID requesterId) {
        return users.findAll();
    }

    @Override
    public List<AdminAction> actions(UUID requesterId) {
        return invoker.history();
    }

    // ---- changes: each one is a command run by the invoker

    @Override
    public List<SettingView> updateSettings(UUID requesterId, Map<String, String> changes) {
        return invoker.run(requesterId, new UpdateSettingsCommand(settings, requesterId, changes));
    }

    @Override
    public List<SettingView> resetSettings(UUID requesterId) {
        return invoker.run(requesterId, new ResetSettingsCommand(settings, requesterId));
    }

    @Override
    public CatalogOption saveCatalogOption(UUID requesterId, CatalogOption option) {
        return invoker.run(requesterId, new SaveCatalogOptionCommand(catalog, option));
    }

    @Override
    public CatalogOption setCatalogOptionActive(UUID requesterId, CatalogKind kind, String code, boolean active) {
        return invoker.run(requesterId, new SetCatalogOptionActiveCommand(catalog, kind, code, active));
    }

    @Override
    public User changeUserRole(UUID requesterId, UUID targetUserId, UserRole role) {
        return invoker.run(requesterId, new ChangeUserRoleCommand(users, clock, requesterId, targetUserId, role));
    }

    @Override
    public User setUserEnabled(UUID requesterId, UUID targetUserId, boolean enabled) {
        return invoker.run(requesterId,
                new SetUserEnabledCommand(users, clock, requesterId, targetUserId, enabled));
    }
}
