package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.domain.exception.AdminAccessDeniedException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminAccessProxyTest {

    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");
    private static final UUID ADMIN = UUID.fromString("3f2a4b5c-0000-4000-8000-00000000000a");
    private static final UUID BREEDER = UUID.fromString("3f2a4b5c-0000-4000-8000-00000000000b");

    private final AdminOperations real = mock(AdminOperations.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AdminAccessProxy proxy = new AdminAccessProxy(real, users);

    private static User account(UUID id, UserRole role) {
        return User.restore(id, "someone", "Someone", "someone@cuymonitor.local", "hash", null,
                UserStatus.ACTIVE, role, NOW, NOW);
    }

    private void theRequesterIs(UUID id, UserRole role) {
        given(users.findById(id)).willReturn(Optional.of(account(id, role)));
    }

    @Test
    void anAdminGetsForwardedToTheRealService() {
        theRequesterIs(ADMIN, UserRole.ADMIN);
        var option = CatalogOption.active(CatalogKind.BREED, "COLOMBIANO", "Colombiano", null, 9);
        given(real.saveCatalogOption(ADMIN, option)).willReturn(option);

        assertThat(proxy.saveCatalogOption(ADMIN, option)).isEqualTo(option);
        verify(real).saveCatalogOption(ADMIN, option);
    }

    @Test
    void aBreederNeverReachesTheRealService() {
        theRequesterIs(BREEDER, UserRole.BREEDER);

        assertThatThrownBy(() -> proxy.saveCatalogOption(BREEDER,
                CatalogOption.active(CatalogKind.BREED, "COLOMBIANO", "Colombiano", null, 9)))
                .isInstanceOf(AdminAccessDeniedException.class);

        verifyNoInteractions(real);
    }

    @Test
    void anAccountThatDoesNotExistIsRefusedTheSameWay() {
        given(users.findById(BREEDER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> proxy.listUsers(BREEDER))
                .isInstanceOf(AdminAccessDeniedException.class)
                // the same wording as for a breeder, so an outsider learns nothing from it
                .hasMessage("this account can't administer the system");

        verifyNoInteractions(real);
    }

    @Test
    void everyMethodIsChecked() {
        theRequesterIs(BREEDER, UserRole.BREEDER);
        var option = CatalogOption.active(CatalogKind.BREED, "COLOMBIANO", "Colombiano", null, 9);

        assertThatThrownBy(() -> proxy.listCatalog(BREEDER)).isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> proxy.saveCatalogOption(BREEDER, option))
                .isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> proxy.setCatalogOptionActive(BREEDER, CatalogKind.BREED, "TEDDY", false))
                .isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> proxy.listUsers(BREEDER)).isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> proxy.changeUserRole(BREEDER, ADMIN, UserRole.ADMIN))
                .isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> proxy.setUserEnabled(BREEDER, ADMIN, false))
                .isInstanceOf(AdminAccessDeniedException.class);

        verifyNoInteractions(real);
    }

    @Test
    void theRoleIsReadFromTheDatabaseOnEveryCall() {
        theRequesterIs(ADMIN, UserRole.ADMIN);
        given(real.listUsers(ADMIN)).willReturn(List.of());
        proxy.listUsers(ADMIN);

        // demoted a moment later: the next call is refused, without waiting for a token to expire
        given(users.findById(ADMIN)).willReturn(Optional.of(account(ADMIN, UserRole.BREEDER)));

        assertThatThrownBy(() -> proxy.listUsers(ADMIN)).isInstanceOf(AdminAccessDeniedException.class);
        // the first call went through, the second did not: the service saw it once, not twice
        verify(real, times(1)).listUsers(ADMIN);
        verify(users, times(2)).findById(ADMIN);
    }
}
