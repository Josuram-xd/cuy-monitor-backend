package com.cuymonitor.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The camera path through HTTP and the real database: the owner sets it, the agent reads it and reports back,
// and the password is never returned nor stored in plain text.
@AutoConfigureMockMvc
class StationCameraIntegrationTest extends PostgresIntegrationTest {

    private static final String PASSWORD = "s3cret-pass";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theOwnerSetsTheCameraAndTheAgentReadsItAndReports() throws Exception {
        UUID owner = UUID.randomUUID();
        String tag = owner.toString().substring(0, 8);
        jdbc.update("insert into app_user (id, username, full_name, email, password_hash, status, created_at,"
                        + " updated_at) values (?, ?, 'Integración', ?, 'hash', 'ACTIVE', now(), now())",
                owner, "cam" + tag, tag + "@integration.local");
        String cage = "cage-cam-" + tag;
        jdbc.update("insert into cage (code, name, owner_id) values (?, 'Jaula cámara', ?)", cage, owner);
        String equipment = "/api/v1/cages/" + cage + "/equipment";
        String camera = equipment + "/camera";
        var session = jwt().jwt(token -> token.subject(owner.toString()));
        var foreign = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()));
        String rtsp = """
                {"type":"RTSP","host":"192.168.1.60","port":null,"username":"admin","password":"%s","stream":"stream2"}
                """.formatted(PASSWORD);

        mvc.perform(put(equipment).with(session)).andExpect(status().isNoContent());
        try {
            mvc.perform(put(camera).with(foreign).contentType(MediaType.APPLICATION_JSON).content(rtsp))
                    .andExpect(status().isForbidden());
            mvc.perform(put(camera).with(session).contentType(MediaType.APPLICATION_JSON)
                            .content(rtsp.replace("192.168.1.60", "8.8.8.8")))
                    .andExpect(status().isBadRequest());
            mvc.perform(put(camera).with(session).contentType(MediaType.APPLICATION_JSON).content(rtsp))
                    .andExpect(status().isNoContent());

            mvc.perform(get(camera).with(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.hasPassword").value(true))
                    .andExpect(jsonPath("$.password").doesNotExist());

            mvc.perform(get("/api/v1/ingestion/station/camera")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/ingestion/station/camera").header("X-API-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("rtsp://admin:" + PASSWORD + "@192.168.1.60:554/stream2"));

            int version = jdbc.queryForObject("select camera_version from hardware_station", Integer.class);
            String stored = jdbc.queryForObject("select camera_password_enc from hardware_station", String.class);
            assertThat(stored).doesNotContain(PASSWORD);

            mvc.perform(post("/api/v1/ingestion/station/camera/status").header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"version\":" + (version - 1) + ",\"status\":\"AUTH_FAILED\"}"))
                    .andExpect(status().isNoContent());
            mvc.perform(get(camera).with(session)).andExpect(jsonPath("$.status").value("PENDING"));

            mvc.perform(post("/api/v1/ingestion/station/camera/status").header("X-API-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"version\":" + version + ",\"status\":\"CONNECTED\"}"))
                    .andExpect(status().isNoContent());
            mvc.perform(get(camera).with(session)).andExpect(jsonPath("$.status").value("CONNECTED"));
        } finally {
            mvc.perform(delete(equipment).with(session)).andExpect(status().isNoContent());
            jdbc.update("update hardware_station set camera_type=null, camera_host=null, camera_port=null,"
                    + " camera_username=null, camera_password_enc=null, camera_stream=null, camera_version=0,"
                    + " camera_status=null, camera_status_at=null");
        }
    }
}
