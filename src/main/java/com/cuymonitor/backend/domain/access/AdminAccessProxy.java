package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AdminAction;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import java.util.Map;
import com.cuymonitor.backend.domain.exception.AdminAccessDeniedException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.port.out.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * <b>Proxy pattern — Proxy (protection proxy).</b> Implements the same {@link AdminOperations} interface as the
 * real subject and keeps a reference to it. Before forwarding any call it checks that the account asking is an
 * admin, and throws {@link AdminAccessDeniedException} if it is not.
 *
 * <p>Written by hand, like {@link CageAccessProxy}: no {@code @PreAuthorize}, no Spring AOP. The check is a
 * method call you can read and step through, and {@code HexagonalArchitectureTest} keeps it that way.
 *
 * <p>The role is read from the database on every call rather than taken from the token: an admin demoted a
 * minute ago must stop being one now, not when their token expires.
 */
public final class AdminAccessProxy implements AdminOperations {

    private final AdminOperations real;
    private final UserRepository users;

    public AdminAccessProxy(AdminOperations real, UserRepository users) {
        this.real = Objects.requireNonNull(real, "real");
        this.users = Objects.requireNonNull(users, "users");
    }

    @Override
    public List<CatalogOption> listCatalog(UUID requesterId) {
        requireAdmin(requesterId);
        return real.listCatalog(requesterId);
    }

    @Override
    public CatalogOption saveCatalogOption(UUID requesterId, CatalogOption option) {
        requireAdmin(requesterId);
        return real.saveCatalogOption(requesterId, option);
    }

    @Override
    public CatalogOption setCatalogOptionActive(UUID requesterId, CatalogKind kind, String code, boolean active) {
        requireAdmin(requesterId);
        return real.setCatalogOptionActive(requesterId, kind, code, active);
    }

    @Override
    public List<User> listUsers(UUID requesterId) {
        requireAdmin(requesterId);
        return real.listUsers(requesterId);
    }

    @Override
    public User changeUserRole(UUID requesterId, UUID targetUserId, UserRole role) {
        requireAdmin(requesterId);
        return real.changeUserRole(requesterId, targetUserId, role);
    }

    @Override
    public Map<UUID, AccountActivity> accountActivity(UUID requesterId) {
        requireAdmin(requesterId);
        return real.accountActivity(requesterId);
    }

    @Override
    public List<SettingView> settings(UUID requesterId) {
        requireAdmin(requesterId);
        return real.settings(requesterId);
    }

    @Override
    public List<SettingView> updateSettings(UUID requesterId, Map<String, String> changes) {
        requireAdmin(requesterId);
        return real.updateSettings(requesterId, changes);
    }

    @Override
    public List<SettingView> resetSettings(UUID requesterId) {
        requireAdmin(requesterId);
        return real.resetSettings(requesterId);
    }

    @Override
    public List<AdminAction> actions(UUID requesterId) {
        requireAdmin(requesterId);
        return real.actions(requesterId);
    }

    @Override
    public List<AiLogEntry> aiLog(UUID requesterId, int limit) {
        requireAdmin(requesterId);
        return real.aiLog(requesterId, limit);
    }

    @Override
    public User setUserEnabled(UUID requesterId, UUID targetUserId, boolean enabled) {
        requireAdmin(requesterId);
        return real.setUserEnabled(requesterId, targetUserId, enabled);
    }

    private void requireAdmin(UUID requesterId) {
        Objects.requireNonNull(requesterId, "requesterId");
        boolean admin = users.findById(requesterId).map(User::isAdmin).orElse(false);
        if (!admin) {
            // the same message whether the account is a breeder or does not exist: an outsider learns nothing
            throw new AdminAccessDeniedException("this account can't administer the system");
        }
    }
}
