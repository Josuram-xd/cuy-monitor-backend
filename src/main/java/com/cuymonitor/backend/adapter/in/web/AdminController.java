package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.model.settings.SettingView;
import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.domain.model.user.User;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;
import com.cuymonitor.backend.adapter.in.web.dto.AdminCatalogOptionRequest;
import com.cuymonitor.backend.adapter.in.web.dto.AdminCatalogOptionResponse;
import com.cuymonitor.backend.adapter.in.web.dto.AdminUserResponse;
import com.cuymonitor.backend.domain.access.AdminOperations;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.user.UserRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * The admin panel. Everything here goes through {@link AdminOperations}, whose only bean is the protection
 * proxy, so the admin check happens before any of these methods reach a service — there is no annotation on
 * this class doing it, and no way to bypass it either.
 *
 * <p>The account asking always comes from the session, never from the body or the URL.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminOperations admin;

    public AdminController(AdminOperations admin) {
        this.admin = admin;
    }

    @GetMapping("/catalog")
    public List<AdminCatalogOptionResponse> catalog(Principal principal) {
        return admin.listCatalog(Requester.idOf(principal)).stream()
                .map(AdminCatalogOptionResponse::from)
                .toList();
    }

    /** Creates the option, or overwrites the one with the same kind and code. */
    @PutMapping("/catalog")
    public AdminCatalogOptionResponse saveOption(Principal principal,
                                                 @Valid @RequestBody AdminCatalogOptionRequest request) {
        return AdminCatalogOptionResponse.from(
                admin.saveCatalogOption(Requester.idOf(principal), request.toOption()));
    }

    /**
     * Switches an option off. It is not deleted: the guinea pigs already registered with it keep their breed or
     * color, they just can't be picked any more. {@code PUT} with {@code active: true} brings it back.
     */
    @DeleteMapping("/catalog/{kind}/{code}")
    public AdminCatalogOptionResponse deactivateOption(Principal principal, @PathVariable CatalogKind kind,
                                                       @PathVariable String code) {
        return AdminCatalogOptionResponse.from(
                admin.setCatalogOptionActive(Requester.idOf(principal), kind, code, false));
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(Principal principal) {
        UUID requesterId = Requester.idOf(principal);
        Map<UUID, AccountActivity> activity = admin.accountActivity(requesterId);
        return admin.listUsers(requesterId).stream()
                .map(user -> AdminUserResponse.from(user,
                        activity.getOrDefault(user.getId(), AccountActivity.none(user.getId()))))
                .toList();
    }

    /** "Reglas y mensajes": every setting with its value, default and range. */
    @GetMapping("/settings")
    public List<SettingView> settings(Principal principal) {
        return admin.settings(Requester.idOf(principal));
    }

    /** Changes the settings sent ({"health.observedToAlertMinutes": "20"}); the rest stay. */
    @PutMapping("/settings")
    public List<SettingView> updateSettings(Principal principal, @RequestBody Map<String, String> changes) {
        return admin.updateSettings(Requester.idOf(principal), changes);
    }

    @DeleteMapping("/settings")
    public List<SettingView> resetSettings(Principal principal) {
        return admin.resetSettings(Requester.idOf(principal));
    }

    /** What the AI did lately (windows analysed, sounds classified, notes on alerts), newest first. */
    @GetMapping("/ai-log")
    public List<AiLogEntry> aiLog(Principal principal, @RequestParam(defaultValue = "100") int limit) {
        return admin.aiLog(Requester.idOf(principal), limit);
    }

    @PatchMapping("/users/{id}")
    public AdminUserResponse updateUser(Principal principal, @PathVariable UUID id,
                                        @Valid @RequestBody AdminUserUpdateRequest request) {
        UUID requesterId = Requester.idOf(principal);
        User changed = request.role() != null
                ? admin.changeUserRole(requesterId, id, request.role())
                : admin.setUserEnabled(requesterId, id, request.isEnabled());
        return AdminUserResponse.from(changed,
                admin.accountActivity(requesterId).getOrDefault(id, AccountActivity.none(id)));
    }

    /** Either a new role or whether the account is switched on; sending neither is a bad request. */
    public record AdminUserUpdateRequest(UserRole role, Boolean enabled) {

        public AdminUserUpdateRequest {
            if (role == null && enabled == null) {
                throw new IllegalArgumentException("send a role or enabled");
            }
        }

        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }
}
