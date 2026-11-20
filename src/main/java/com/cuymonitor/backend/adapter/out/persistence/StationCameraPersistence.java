package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.domain.model.CameraStatus;
import com.cuymonitor.backend.domain.model.StationCamera;
import com.cuymonitor.backend.domain.port.out.StationCameraRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Optional;

@Component
public class StationCameraPersistence implements StationCameraRepository {
    private final JdbcTemplate jdbc;
    private final CameraPasswordCipher cipher;

    public StationCameraPersistence(JdbcTemplate jdbc, CameraPasswordCipher cipher) {
        this.jdbc = jdbc;
        this.cipher = cipher;
    }

    @Override
    public Optional<StationCamera> find() {
        return jdbc.query("""
                select camera_type, camera_host, camera_port, camera_username, camera_password_enc, camera_stream,
                  camera_version, camera_status, camera_status_at
                from hardware_station where camera_type is not null
                """, (rs, row) -> {
            String status = rs.getString("camera_status");
            Timestamp statusAt = rs.getTimestamp("camera_status_at");
            return new StationCamera(rs.getString("camera_type"), rs.getString("camera_host"), rs.getInt("camera_port"),
                    rs.getString("camera_username"), cipher.decrypt(rs.getString("camera_password_enc")).orElse(null),
                    rs.getString("camera_stream"), rs.getInt("camera_version"),
                    status == null ? null : CameraStatus.valueOf(status),
                    statusAt == null ? null : statusAt.toInstant());
        }).stream().findFirst();
    }

    @Override
    public void save(String type, String host, int port, String username, String password, String stream) {
        jdbc.update("""
                update hardware_station set camera_type=?, camera_host=?, camera_port=?, camera_username=?,
                  camera_password_enc=?, camera_stream=?, camera_version=camera_version+1,
                  camera_status=null, camera_status_at=null
                where singleton
                """, type, host, port, username, password == null ? null : cipher.encrypt(password), stream);
    }

    @Override
    public boolean report(int version, CameraStatus status) {
        return jdbc.update("""
                update hardware_station set camera_status=?, camera_status_at=clock_timestamp()
                where camera_version=? and camera_type is not null
                """, status.name(), version) == 1;
    }
}
