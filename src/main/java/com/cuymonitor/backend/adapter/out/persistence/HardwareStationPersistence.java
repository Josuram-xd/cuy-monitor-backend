package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.domain.port.out.HardwareStation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HardwareStationPersistence implements HardwareStation {
    private final JdbcTemplate jdbc;

    public HardwareStationPersistence(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Binding> current() {
        return jdbc.query("select c.code, s.since from hardware_station s join cage c on c.id=s.cage_id where c.active",
                (rs, row) -> new Binding(rs.getString(1), rs.getTimestamp(2).toInstant())).stream().findFirst();
    }

    public boolean connect(String cageCode) {
        return jdbc.update("""
                update hardware_station s set cage_id=c.id,
                  since=case when s.cage_id=c.id then s.since else clock_timestamp() end
                from cage c where c.code=? and c.active and
                  (s.cage_id=c.id or not exists(select 1 from cage old where old.id=s.cage_id and old.active))
                """, cageCode) == 1;
    }

    public void disconnect(String cageCode) {
        jdbc.update("update hardware_station set cage_id=null, since=null where cage_id=(select id from cage where code=?)",
                cageCode);
    }
}
