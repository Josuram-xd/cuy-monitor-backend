package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.AuthToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookiesTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AuthSession SESSION = new AuthSession(new AuthToken("jwt", NOW.plusSeconds(900)), "refresh",
            NOW.plusSeconds(604_800));

    @Test
    void theSessionCookiesAreHttpOnlySecureAndStrict() {
        List<String> cookies = new AuthCookies(true, CLOCK).issue(SESSION).get(HttpHeaders.SET_COOKIE);

        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains("access_token=jwt", "HttpOnly", "Secure", "SameSite=Strict", "Path=/",
                "Max-Age=900");
        assertThat(cookies.get(1)).contains("refresh_token=refresh", "HttpOnly", "Secure", "SameSite=Strict",
                "Path=/api/v1/auth", "Max-Age=604800");
    }

    @Test
    void secureCanBeTurnedOffForPlainHttp() {
        List<String> cookies = new AuthCookies(false, CLOCK).issue(SESSION).get(HttpHeaders.SET_COOKIE);

        assertThat(cookies).noneMatch(cookie -> cookie.contains("Secure"));
        assertThat(cookies).allMatch(cookie -> cookie.contains("HttpOnly"));
    }

    @Test
    void clearingExpiresBothCookiesOnTheSamePaths() {
        List<String> cookies = new AuthCookies(true, CLOCK).clear().get(HttpHeaders.SET_COOKIE);

        assertThat(cookies.get(0)).contains("access_token=", "Max-Age=0", "Path=/");
        assertThat(cookies.get(1)).contains("refresh_token=", "Max-Age=0", "Path=/api/v1/auth");
    }
}
