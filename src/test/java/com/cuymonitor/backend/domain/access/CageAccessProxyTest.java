package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.domain.exception.AlertNotFoundException;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proxy pattern: the proxy must forward to the real subject only when the requester owns the cage. The real subject
 * here is a fake that only records which calls reached it, so each test shows exactly what the proxy let through.
 */
class CageAccessProxyTest {

    private static final Instant NOW = Instant.parse("2026-11-02T14:00:00Z");
    private static final UUID ANA = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    private static final UUID LUIS = UUID.fromString("00000000-0000-4000-8000-00000000000b");

    private RecordingRealSubject realSubject;
    private InMemoryCageRepository cages;
    private InMemoryAlertRepository alerts;
    private CageOperations proxy;

    @BeforeEach
    void setUp() {
        realSubject = new RecordingRealSubject();
        cages = new InMemoryCageRepository();
        alerts = new InMemoryAlertRepository();
        cages.save(new Cage(null, "cage-ana", "Jaula de Ana", null, ANA, true, NOW));
        cages.save(new Cage(null, "cage-luis", "Jaula de Luis", null, LUIS, true, NOW));
        proxy = new CageAccessProxy(realSubject, cages, alerts);
    }

    @Test
    void theProxyIsACageOperationsLikeTheRealSubject() {
        assertThat(proxy).isInstanceOf(CageOperations.class);
    }

    @Test
    void forwardsEveryCallOnACageTheRequesterOwns() {
        proxy.getCage(ANA, "cage-ana");
        proxy.updateCage(ANA, "cage-ana", "Nueva", null);
        proxy.getHealth(ANA, "cage-ana");
        proxy.listGuineaPigs(ANA, "cage-ana");
        proxy.registerGuineaPig(ANA, "cage-ana", "RED", Photos.png(), GuineaPigProfile.EMPTY);
        proxy.getGuineaPigPhoto(ANA, "cage-ana", 7L);
        proxy.deleteGuineaPig(ANA, "cage-ana", 7L);
        proxy.listAlerts(ANA, "cage-ana", AlertStatus.OPEN);
        proxy.deleteCage(ANA, "cage-ana");

        assertThat(realSubject.calls).containsExactly("getCage cage-ana", "updateCage cage-ana", "getHealth cage-ana",
                "listGuineaPigs cage-ana", "registerGuineaPig cage-ana", "getGuineaPigPhoto cage-ana 7",
                "deleteGuineaPig cage-ana 7",
                "listAlerts cage-ana", "deleteCage cage-ana");
    }

    @Test
    void refusesEveryCallOnAnotherAccountsCageWithoutReachingTheRealSubject() {
        List<Executable> calls = List.of(
                () -> proxy.getCage(ANA, "cage-luis"),
                () -> proxy.updateCage(ANA, "cage-luis", "Mía", null),
                () -> proxy.deleteCage(ANA, "cage-luis"),
                () -> proxy.getHealth(ANA, "cage-luis"),
                () -> proxy.listGuineaPigs(ANA, "cage-luis"),
                () -> proxy.registerGuineaPig(ANA, "cage-luis", "RED", Photos.png(), GuineaPigProfile.EMPTY),
                () -> proxy.getGuineaPigPhoto(ANA, "cage-luis", 1L),
                () -> proxy.deleteGuineaPig(ANA, "cage-luis", 1L),
                () -> proxy.listAlerts(ANA, "cage-luis", null));

        for (Executable call : calls) {
            assertThrows(CageAccessDeniedException.class, call);
        }
        assertThat(realSubject.calls).isEmpty();
    }

    @Test
    void anUnknownOrDeletedCageIsNotFoundAndNeverReachesTheRealSubject() {
        cages.save(cages.findByCode("cage-ana").orElseThrow().deactivate());

        assertThatThrownBy(() -> proxy.getHealth(ANA, "cage-nope")).isInstanceOf(CageNotFoundException.class);
        assertThatThrownBy(() -> proxy.getHealth(ANA, "cage-ana")).isInstanceOf(CageNotFoundException.class);
        assertThat(realSubject.calls).isEmpty();
    }

    @Test
    void aCageWithoutOwnerIsNobodys() {
        // the pilot cage of InMemoryCageRepository has no owner
        assertThatThrownBy(() -> proxy.getCage(ANA, "cage-1")).isInstanceOf(CageAccessDeniedException.class);
    }

    @Test
    void listingAndCreatingOwnCagesGoStraightThrough() {
        proxy.listCages(ANA);
        proxy.createCage(ANA, "Otra", "Patio");

        assertThat(realSubject.calls).containsExactly("listCages " + ANA, "createCage " + ANA);
    }

    @Test
    void listingEveryAlertKeepsOnlyThoseOfTheRequestersCages() {
        realSubject.allAlerts = List.of(
                alert(1L, "cage-ana"), alert(2L, "cage-luis"), alert(3L, "cage-ana"), alert(4L, "cage-1"));

        assertThat(proxy.listAlerts(ANA, null, null)).extracting(Alert::getId).containsExactly(1L, 3L);
        assertThat(proxy.listAlerts(LUIS, null, null)).extracting(Alert::getId).containsExactly(2L);
    }

    @Test
    void onlyTheOwnerOfTheAlertsCageCanReviewIt() {
        Alert ofLuis = alerts.save(Alert.forCage("cage-luis", EventType.AUDIO, HealthStatus.ALERT, "chillidos", NOW));

        assertThatThrownBy(() -> proxy.reviewAlert(ANA, ofLuis.getId()))
                .isInstanceOf(CageAccessDeniedException.class);
        assertThat(realSubject.calls).isEmpty();

        proxy.reviewAlert(LUIS, ofLuis.getId());
        assertThat(realSubject.calls).containsExactly("reviewAlert " + ofLuis.getId());
    }

    @Test
    void reviewingAnUnknownAlertIsNotFound() {
        assertThatThrownBy(() -> proxy.reviewAlert(ANA, 999L)).isInstanceOf(AlertNotFoundException.class);
    }

    private static Alert alert(long id, String cageCode) {
        return Alert.restore(id, cageCode, null, HealthStatus.ALERT, EventType.AUDIO, "x", AlertStatus.OPEN, NOW, null);
    }

    /** Stands in for CageOperationsService: answers with dummies and records each call that reached it. */
    private static final class RecordingRealSubject implements CageOperations {
        final List<String> calls = new ArrayList<>();
        List<Alert> allAlerts = List.of();

        @Override
        public List<Cage> listCages(UUID requesterId) {
            calls.add("listCages " + requesterId);
            return List.of();
        }

        @Override
        public Cage createCage(UUID requesterId, String name, String location) {
            calls.add("createCage " + requesterId);
            return Cage.create("cage-new", name, location, requesterId, NOW);
        }

        @Override
        public Cage getCage(UUID requesterId, String cageCode) {
            calls.add("getCage " + cageCode);
            return new Cage(1L, cageCode, "x");
        }

        @Override
        public Cage updateCage(UUID requesterId, String cageCode, String name, String location) {
            calls.add("updateCage " + cageCode);
            return new Cage(1L, cageCode, name);
        }

        @Override
        public void deleteCage(UUID requesterId, String cageCode) {
            calls.add("deleteCage " + cageCode);
        }

        @Override
        public CageHealthSummary getHealth(UUID requesterId, String cageCode) {
            calls.add("getHealth " + cageCode);
            return null;
        }

        @Override
        public List<GuineaPig> listGuineaPigs(UUID requesterId, String cageCode) {
            calls.add("listGuineaPigs " + cageCode);
            return List.of();
        }

        @Override
        public GuineaPig registerGuineaPig(UUID requesterId, String cageCode, String markColor,
                                          GuineaPigPhoto photo, GuineaPigProfile profile) {
            calls.add("registerGuineaPig " + cageCode);
            return null;
        }

        @Override
        public GuineaPigPhoto getGuineaPigPhoto(UUID requesterId, String cageCode, long guineaPigId) {
            calls.add("getGuineaPigPhoto " + cageCode + " " + guineaPigId);
            return null;
        }

        @Override
        public void deleteGuineaPig(UUID requesterId, String cageCode, long guineaPigId) {
            calls.add("deleteGuineaPig " + cageCode + " " + guineaPigId);
        }

        @Override
        public List<Alert> listAlerts(UUID requesterId, String cageCode, AlertStatus status) {
            if (cageCode != null) {
                calls.add("listAlerts " + cageCode);
            }
            return allAlerts;
        }

        @Override
        public Alert reviewAlert(UUID requesterId, long alertId) {
            calls.add("reviewAlert " + alertId);
            return null;
        }
    }
}
