package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StationCameraTest {

    private static StationCamera camera(String user, String password) {
        return new StationCamera("RTSP", "192.168.1.60", 554, user, password, "stream2", 1, null, Instant.EPOCH);
    }

    @Test
    void thePhoneIsItsWebAddress() {
        var phone = new StationCamera("PHONE", "192.168.1.60", 8080, null, null, null, 1, null, null);

        assertEquals("http://192.168.1.60:8080", phone.url());
    }

    @Test
    void theRtspUrlCarriesTheCredentialsAndTheStream() {
        assertEquals("rtsp://admin:secret@192.168.1.60:554/stream2", camera("admin", "secret").url());
        assertEquals("rtsp://admin@192.168.1.60:554/stream2", camera("admin", null).url());
    }

    @Test
    void specialCharactersArePercentEncoded() {
        assertEquals("rtsp://us%40er:p%3Aa%20ss%40@192.168.1.60:554/stream2", camera("us@er", "p:a ss@").url());
    }

    @Test
    void theToStringNeverShowsThePassword() {
        assertFalse(camera("admin", "secret").toString().contains("secret"));
    }
}
