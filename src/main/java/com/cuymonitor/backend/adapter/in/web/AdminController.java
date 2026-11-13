package com.cuymonitor.backend.adapter.in.web;

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
        return admin.listUsers(Requester.idOf(principal)).stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @PatchMapping("/users/{id}")
    public AdminUserResponse updateUser(Principal principal, @PathVariable UUID id,
                                        @Valid @RequestBody AdminUserUpdateRequest request) {
        UUID requesterId = Requester.idOf(principal);
        if (request.role() != null) {
            return AdminUserResponse.from(admin.changeUserRole(requesterId, id, request.role()));
        }
        return AdminUserResponse.from(admin.setUserEnabled(requesterId, id, request.isEnabled()));
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
