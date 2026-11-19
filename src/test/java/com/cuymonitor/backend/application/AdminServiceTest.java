package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.port.out.SettingRepository;
import java.util.Map;
import java.util.List;
import com.cuymonitor.backend.domain.port.out.AdminReportRepository;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AdminAction;
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
    private final AdminReportRepository reports = new AdminReportRepository() {
        @Override
        public Map<UUID, AccountActivity> activityByAccount() {
            return Map.of();
        }

        @Override
        public List<AiLogEntry> latestAiLog(int limit) {
            return List.of(new AiLogEntry(NOW, "cage-1", AiLogEntry.Kind.NOTE, "nota"));
        }
    };
    private final AdminService service = new AdminService(catalog, users, clock, reports,
            new SettingsService(SettingsServiceTest.defaults(), new SettingRepository() {
                @Override
                public Map<String, String> findAll() {
                    return Map.of();
                }

                @Override
                public void replaceAll(Map<String, String> changes, UUID changedBy) {
                }
            }, List.of()));

    @Test
    void aBadSettingIsABadRequestForTheAdmin() {
        assertThatThrownBy(() -> service.updateSettings(admin, Map.of("health.recoveryWindows", "0")))
                .isInstanceOf(InvalidAdminActionException.class);
        assertThat(service.updateSettings(admin, Map.of("health.recoveryWindows", "4")))
                .anySatisfy(v -> assertThat(v.value()).isEqualTo("4"));
    }

    @Test
    void theAiLogShowsBetweenOneAndTwoHundredLines() {
        assertThat(service.aiLog(admin, 50)).hasSize(1);
        assertThatThrownBy(() -> service.aiLog(admin, 0)).isInstanceOf(InvalidAdminActionException.class);
        assertThatThrownBy(() -> service.aiLog(admin, 201)).isInstanceOf(InvalidAdminActionException.class);
    }

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

    // The two below call the service straight, without AdminAccessProxy. Through the API nobody reaches
    // this state: demoting the only admin takes an admin, and the only admin is you, which is refused above.
    // They cover the service as a safety net for a caller that does not go through the proxy.
    @Test
    void theLastAdminCannotBeDemotedEvenWithoutTheProxy() {
        UUID second = save("otro-admin", UserRole.ADMIN, UserStatus.ACTIVE);
        // with two admins, demoting one is fine
        service.changeUserRole(admin, second, UserRole.BREEDER);

        // now there is one left, and nobody could edit the catalog again
        assertThatThrownBy(() -> service.changeUserRole(second, admin, UserRole.BREEDER))
                .isInstanceOf(InvalidAdminActionException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void theLastAdminCannotBeSwitchedOffEitherWithoutTheProxy() {
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

    // --- Command pattern: every change goes through the invoker, which remembers it ---

    @Test
    void theHistoryKeepsEveryChangeThatWentThroughNewestFirst() {
        service.setUserEnabled(admin, breeder, false);
        clock.advance(java.time.Duration.ofMinutes(1));
        service.setCatalogOptionActive(admin, CatalogKind.BREED, "teddy", false);

        assertThat(service.actions(admin))
                .extracting(AdminAction::description)
                .containsExactly("switch off BREED TEDDY", "disable account " + breeder);
        assertThat(service.actions(admin).get(0).adminId()).isEqualTo(admin);
        assertThat(service.actions(admin).get(0).at()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void aRefusedChangeIsNotHistory() {
        assertThatThrownBy(() -> service.setUserEnabled(admin, admin, false))
                .isInstanceOf(InvalidAdminActionException.class);

        assertThat(service.actions(admin)).isEmpty();
    }

    @Test
    void readsAreNotCommands() {
        service.listUsers(admin);
        service.listCatalog(admin);
        service.settings(admin);

        assertThat(service.actions(admin)).isEmpty();
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
