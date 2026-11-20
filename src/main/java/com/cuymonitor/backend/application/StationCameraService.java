package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.EquipmentNotConnectedException;
import com.cuymonitor.backend.domain.exception.InvalidCameraSettingsException;
import com.cuymonitor.backend.domain.model.CameraStatus;
import com.cuymonitor.backend.domain.model.StationCamera;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase;
import com.cuymonitor.backend.domain.port.out.HardwareStation;
import com.cuymonitor.backend.domain.port.out.StationCameraRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StationCameraService implements ConfigureStationCameraUseCase {

    // the edge agent reports every 15 s; a report older than this means the agent is not running
    static final Duration OFFLINE_AFTER = Duration.ofSeconds(60);

    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
    private static final Set<String> TYPES = Set.of("PHONE", "RTSP");
    private static final Set<String> STREAMS = Set.of("stream1", "stream2");

    private final StationCameraRepository cameras;
    private final HardwareStation station;
    private final Clock clock;

    public StationCameraService(StationCameraRepository cameras, HardwareStation station, Clock clock) {
        this.cameras = Objects.requireNonNull(cameras, "cameras");
        this.station = Objects.requireNonNull(station, "station");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CameraView view(String cageCode) {
        requireConnected(cageCode);
        return cameras.find()
                .map(camera -> new CameraView(camera.type(), camera.host(), camera.port(), camera.username(),
                        camera.stream(), camera.password() != null, statusOf(camera), camera.reportedAt()))
                .orElse(new CameraView(null, null, null, null, null, false, CameraStatus.NOT_CONFIGURED, null));
    }

    @Override
    public void save(String cageCode, CameraSettings input) {
        requireConnected(cageCode);
        if (input == null || input.type() == null || !TYPES.contains(input.type())) {
            throw new InvalidCameraSettingsException("type must be PHONE or RTSP");
        }
        String host = input.host() == null ? "" : input.host().trim();
        if (!isPrivateIpv4(host)) {
            throw new InvalidCameraSettingsException("host must be a private IPv4 address (10.x, 172.16-31.x or 192.168.x)");
        }
        boolean rtsp = "RTSP".equals(input.type());
        int port = input.port() != null ? input.port() : (rtsp ? 554 : 8080);
        if (port < 1 || port > 65535) {
            throw new InvalidCameraSettingsException("port must be between 1 and 65535");
        }
        if (!rtsp) {
            cameras.save("PHONE", host, port, null, null, null);
            return;
        }

        String username = input.username() == null ? "" : input.username().trim();
        if (username.isEmpty() || username.length() > 64 || hasControlCharacters(username)) {
            throw new InvalidCameraSettingsException("username must be 1 to 64 characters");
        }
        String stream = input.stream() == null ? "stream2" : input.stream();
        if (!STREAMS.contains(stream)) {
            throw new InvalidCameraSettingsException("stream must be stream1 or stream2");
        }
        String password = input.password();
        if (password == null || password.isBlank()) {
            password = cameras.find().map(StationCamera::password)
                    .orElseThrow(() -> new InvalidCameraSettingsException("password is required"));
        } else if (password.length() > 64) {
            throw new InvalidCameraSettingsException("password must be at most 64 characters");
        }
        cameras.save("RTSP", host, port, username, password, stream);
    }

    @Override
    public Optional<StationCamera> forAgent() {
        return cameras.find();
    }

    @Override
    public void report(int version, CameraStatus status) {
        if (!CameraStatus.REPORTABLE.contains(status)) {
            throw new InvalidCameraSettingsException("status must be CONNECTED, AUTH_FAILED, UNREACHABLE or FFMPEG_MISSING");
        }
        // a false return means the agent reported a configuration that was already replaced: ignored on purpose
        cameras.report(version, status);
    }

    private CameraStatus statusOf(StationCamera camera) {
        if (camera.reported() == null) {
            return CameraStatus.PENDING;
        }
        boolean stale = Duration.between(camera.reportedAt(), clock.instant()).compareTo(OFFLINE_AFTER) > 0;
        return stale ? CameraStatus.OFFLINE : camera.reported();
    }

    private void requireConnected(String cageCode) {
        if (!station.connectedTo(cageCode)) {
            throw new EquipmentNotConnectedException();
        }
    }

    // private network only: the kit is shared, so the dashboard must not point the laptop at outside addresses
    static boolean isPrivateIpv4(String host) {
        Matcher matcher = IPV4.matcher(host);
        if (!matcher.matches()) {
            return false;
        }
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            octets[i] = Integer.parseInt(matcher.group(i + 1));
            if (octets[i] > 255) {
                return false;
            }
        }
        return octets[0] == 10
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
    }

    private static boolean hasControlCharacters(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }
}
