package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.EquipmentNotConnectedException;
import com.cuymonitor.backend.domain.exception.InvalidCameraSettingsException;
import com.cuymonitor.backend.domain.model.CameraStatus;
import com.cuymonitor.backend.domain.model.StationCamera;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase.CameraSettings;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase.CameraView;
import com.cuymonitor.backend.domain.port.out.HardwareStation;
import com.cuymonitor.backend.domain.port.out.StationCameraRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StationCameraServiceTest {

    private static final String CAGE = "cage-1";

    private static final class FakeStation implements HardwareStation {
        String bound = CAGE;

        public Optional<Binding> current() {
            return Optional.ofNullable(bound).map(code -> new Binding(code, Instant.EPOCH));
        }

        public boolean connect(String cageCode) {
            bound = cageCode;
            return true;
        }

        public void disconnect(String cageCode) {
            if (cageCode.equals(bound)) bound = null;
        }
    }

    private static final class FakeCameras implements StationCameraRepository {
        StationCamera stored;
        private final Clock clock;

        FakeCameras(Clock clock) {
            this.clock = clock;
        }

        public Optional<StationCamera> find() {
            return Optional.ofNullable(stored);
        }

        public void save(String type, String host, int port, String username, String password, String stream) {
            int version = stored == null ? 1 : stored.version() + 1;
            stored = new StationCamera(type, host, port, username, password, stream, version, null, null);
        }

        public boolean report(int version, CameraStatus status) {
            if (stored == null || stored.version() != version) return false;
            stored = new StationCamera(stored.type(), stored.host(), stored.port(), stored.username(),
                    stored.password(), stored.stream(), stored.version(), status, clock.instant());
            return true;
        }
    }

    // a clock the test moves by hand, to check the OFFLINE rule
    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-20T12:00:00Z");

        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now;
        }
    }

    private final FakeStation station = new FakeStation();
    private final MutableClock clock = new MutableClock();
    private final FakeCameras cameras = new FakeCameras(clock);
    private final StationCameraService service = new StationCameraService(cameras, station, clock);

    private static CameraSettings rtsp(String host, String password) {
        return new CameraSettings("RTSP", host, null, "admin", password, null);
    }

    @Test
    void theCameraBelongsToTheCageTheKitIsConnectedTo() {
        station.bound = "cage-2";

        assertThrows(EquipmentNotConnectedException.class, () -> service.save(CAGE, rtsp("192.168.1.60", "pw")));
        assertThrows(EquipmentNotConnectedException.class, () -> service.view(CAGE));
    }

    @Test
    void onlyPrivateIpv4AddressesAreAccepted() {
        for (String host : new String[]{"192.168.1.60", "10.0.0.5", "172.20.0.2", "172.16.0.1", "172.31.255.1"}) {
            assertDoesNotThrow(() -> service.save(CAGE, rtsp(host, "pw")), host);
        }
        for (String host : new String[]{"8.8.8.8", "999.1.1.1", "172.32.0.1", "172.15.0.1", "example.com", ""}) {
            assertThrows(InvalidCameraSettingsException.class, () -> service.save(CAGE, rtsp(host, "pw")), host);
        }
    }

    @Test
    void defaultsAreThePortOfEachTypeAndStream2() {
        service.save(CAGE, new CameraSettings("PHONE", "192.168.1.60", null, null, null, null));
        assertEquals(8080, cameras.stored.port());
        assertNull(cameras.stored.username());
        assertNull(cameras.stored.stream());

        service.save(CAGE, rtsp("192.168.1.60", "pw"));
        assertEquals(554, cameras.stored.port());
        assertEquals("stream2", cameras.stored.stream());
    }

    @Test
    void aBlankPasswordKeepsTheStoredOne() {
        service.save(CAGE, rtsp("192.168.1.60", "secret"));
        service.save(CAGE, rtsp("192.168.1.61", ""));

        assertEquals("secret", cameras.stored.password());
        assertEquals("192.168.1.61", cameras.stored.host());
        assertEquals(2, cameras.stored.version());
    }

    @Test
    void rtspWithoutAnyStoredPasswordIsRefused() {
        assertThrows(InvalidCameraSettingsException.class, () -> service.save(CAGE, rtsp("192.168.1.60", null)));
        assertNull(cameras.stored);
    }

    @Test
    void theStatusFollowsTheAgentAndGoesOfflineAfterAMinute() {
        service.save(CAGE, rtsp("192.168.1.60", "pw"));
        assertEquals(CameraStatus.PENDING, service.view(CAGE).status());

        cameras.report(cameras.stored.version(), CameraStatus.CONNECTED);
        assertEquals(CameraStatus.CONNECTED, service.view(CAGE).status());

        clock.now = clock.now.plusSeconds(61);
        assertEquals(CameraStatus.OFFLINE, service.view(CAGE).status());
    }

    @Test
    void aLateReportForAnOlderVersionChangesNothing() {
        service.save(CAGE, rtsp("192.168.1.60", "pw"));
        int old = cameras.stored.version();
        service.save(CAGE, rtsp("192.168.1.61", "pw"));

        service.report(old, CameraStatus.CONNECTED);

        assertEquals(CameraStatus.PENDING, service.view(CAGE).status());
    }

    @Test
    void theDashboardViewOnlyTellsWhetherThereIsAPassword() {
        CameraView before = service.view(CAGE);
        assertEquals(CameraStatus.NOT_CONFIGURED, before.status());
        assertFalse(before.hasPassword());

        service.save(CAGE, rtsp("192.168.1.60", "pw"));
        CameraView after = service.view(CAGE);
        assertTrue(after.hasPassword());
        assertEquals("192.168.1.60", after.host());
    }

    @Test
    void reportsOnlyTheStatusesTheAgentCanSend() {
        service.save(CAGE, rtsp("192.168.1.60", "pw"));

        assertThrows(InvalidCameraSettingsException.class,
                () -> service.report(cameras.stored.version(), CameraStatus.OFFLINE));
        assertThrows(InvalidCameraSettingsException.class,
                () -> service.report(cameras.stored.version(), null));
    }
}
