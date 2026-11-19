package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AdminAction;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the admin panel can do. The real subject of the Proxy pattern: {@link AdminAccessProxy} implements
 * this same interface and checks the caller is an admin before forwarding. Controllers only ever receive this
 * type, so there is no way to reach the real service without passing the check.
 *
 * <p>Every method takes the account asking, because the check is per call: a session does not stay trusted.
 */
public interface AdminOperations {

    /** The whole catalog, including the options that are switched off. */
    List<CatalogOption> listCatalog(UUID requesterId);

    /** Adds an option, or overwrites the one with the same kind and code. */
    CatalogOption saveCatalogOption(UUID requesterId, CatalogOption option);

    /** Hides an option without touching the guinea pigs already using it. */
    CatalogOption setCatalogOptionActive(UUID requesterId, CatalogKind kind, String code, boolean active);

    List<User> listUsers(UUID requesterId);

    User changeUserRole(UUID requesterId, UUID targetUserId, UserRole role);

    User setUserEnabled(UUID requesterId, UUID targetUserId, boolean enabled);

    /** A summary per account (cages, guinea pigs, open alerts, last data): never what is inside the cages. */
    Map<UUID, AccountActivity> accountActivity(UUID requesterId);

    /** What the AI did lately, newest first, at most limit lines. */
    List<AiLogEntry> aiLog(UUID requesterId, int limit);

    /** The health rules, alert texts and assistant limit: their values, defaults and ranges. */
    List<SettingView> settings(UUID requesterId);

    /** Changes some of them; one value out of range changes nothing. Applied at once, no restart. */
    List<SettingView> updateSettings(UUID requesterId, Map<String, String> changes);

    /** Back to the defaults. */
    List<SettingView> resetSettings(UUID requesterId);

    /** The admin changes that went through since the last restart, newest first (Command pattern history). */
    List<AdminAction> actions(UUID requesterId);
}
