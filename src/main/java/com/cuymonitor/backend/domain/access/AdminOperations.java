package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;

import java.util.List;
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
}
