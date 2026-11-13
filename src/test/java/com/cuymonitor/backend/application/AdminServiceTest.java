package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakeCatalogOptionRepository;
import com.cuymonitor.backend.application.fake.InMemoryUserRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.exception.UserNotFoundException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    private final FakeCatalogOptionRepository catalog = new FakeCatalogOptionRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final MutableClock clock = new MutableClock(NOW);
    private final AdminService service = new AdminService(catalog, users, clock);

    private UUID admin;
    private UUID breeder;

    private UUID save(String username, UserRole role, UserStatus status) {
        UUID id = UUID.randomUUID();
        users.save(User.restore(id, username, username, username + "@cuymonitor.local", "hash", null, status,
                role, NOW, NOW));
        return id;
    }

    @BeforeEach
    void setUp() {
        admin = save("admin", UserRole.ADMIN, UserStatus.ACTIVE);
        breeder = save("criador", UserRole.BREEDER, UserStatus.ACTIVE);
    }

    // --- the catalog: what the professor actually asked for ---

    @Test
    void addsABreedThatDidNotExist() {
        var colombiano = CatalogOption.active(CatalogKind.BREED, "colombiano", "Cuy colombiano", null, 9);

        service.saveCatalogOption(admin, colombiano);

        assertThat(catalog.findAllActive())
                .extracting(CatalogOption::code)
                .contains("COLOMBIANO");
        // typed in lower case, stored upper case: the picker and the database agree
        assertThat(catalog.find(CatalogKind.BREED, "COLOMBIANO")).isPresent();
    }

    @Test
    void refusesACodeTheDatabaseWouldReject() {
        assertThatThrownBy(() -> service.saveCatalogOption(admin,
                CatalogOption.active(CatalogKind.BREED, "CUY COLOMBIANO", "Con espacio", null, 9)))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("letters, numbers and _");
    }

    @Test
    void refusesAColorThatIsNotAHexValue() {
        assertThatThrownBy(() -> service.saveCatalogOption(admin,
                CatalogOption.active(CatalogKind.MARK_COLOR, "TEAL", "Verde azulado", "teal", 9)))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("#a1b2c3");
    }

    @Test
    void switchingAnOptionOffTakesItOutOfThePicker() {
        service.setCatalogOptionActive(admin, CatalogKind.BREED, "TEDDY", false);

        assertThat(catalog.findAllActive()).extracting(CatalogOption::code).doesNotContain("TEDDY");
    }

    @Test
    void switchingOffSomethingThatIsNotThereSaysSo() {
        assertThatThrownBy(() -> service.setCatalogOptionActive(admin, CatalogKind.BREED, "NOPE", false))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("NOPE");
    }

    // --- the accounts, and the ways an admin could lock everybody out ---

    @Test
    void namesAnotherAdmin() {
        service.changeUserRole(admin, breeder, UserRole.ADMIN);

        assertThat(users.findById(breeder).orElseThrow().isAdmin()).isTrue();
        assertThat(users.countAdmins()).isEqualTo(2);
    }

    @Test
    void anAdminCannotChangeTheirOwnRole() {
        assertThatThrownBy(() -> service.changeUserRole(admin, admin, UserRole.BREEDER))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("your own role");
    }

    @Test
    void theLastAdminCannotBeDemoted() {
        UUID second = save("otro-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        // with two admins, demoting one is fine
        service.changeUserRole(admin, second, UserRole.BREEDER);

        // now there is one left, and nobody could edit the catalog again
        assertThatThrownBy(() -> service.changeUserRole(second, admin, UserRole.BREEDER))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void theLastAdminCannotBeSwitchedOffEither() {
        UUID second = save("otro-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        service.changeUserRole(admin, second, UserRole.BREEDER);

        assertThatThrownBy(() -> service.setUserEnabled(second, admin, false))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void anAdminCannotSwitchThemselvesOff() {
        assertThatThrownBy(() -> service.setUserEnabled(admin, admin, false))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("your own account");
    }

    @Test
    void switchesABreederOffAndBackOn() {
        service.setUserEnabled(admin, breeder, false);
        assertThat(users.findById(breeder).orElseThrow().getStatus()).isEqualTo(UserStatus.DISABLED);

        service.setUserEnabled(admin, breeder, true);
        assertThat(users.findById(breeder).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void switchingOffTwiceDoesNotBlowUp() {
        service.setUserEnabled(admin, breeder, false);
        service.setUserEnabled(admin, breeder, false);

        assertThat(users.findById(breeder).orElseThrow().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void anAccountThatIsNotThereIs404() {
        assertThatThrownBy(() -> service.changeUserRole(admin, UUID.randomUUID(), UserRole.ADMIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listsEveryAccount() {
        assertThat(service.listUsers(admin)).hasSize(2);
    }
}
