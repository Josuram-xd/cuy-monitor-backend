package com.cuymonitor.backend.domain.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * The camera of the kit as the edge agent needs it. {@code password} is plain text and lives only in memory: it is
 * never printed and never sent to the dashboard.
 */
public record StationCamera(String type, String host, int port, String username, String password, String stream,
                            int version, CameraStatus reported, Instant reportedAt) {

    /** The address the edge agent opens: the phone's web page, or the RTSP stream of the Tapo. */
    public String url() {
        if ("PHONE".equals(type)) {
            return "http://" + host + ":" + port;
        }
        String credentials = encode(username) + (password == null ? "" : ":" + encode(password));
        return "rtsp://" + credentials + "@" + host + ":" + port + "/" + stream;
    }

    // percent-encoded, so an '@' or ':' in the password does not break the URL
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    public String toString() {
        return "StationCamera[type=" + type + ", host=" + host + ", port=" + port + ", username=" + username
                + ", stream=" + stream + ", version=" + version + ", reported=" + reported
                + ", reportedAt=" + reportedAt + "]";
    }
}
