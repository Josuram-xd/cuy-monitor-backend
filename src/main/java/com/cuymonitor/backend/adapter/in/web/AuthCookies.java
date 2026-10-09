package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.model.auth.AuthSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * The session lives in two HttpOnly cookies, so JavaScript (and DevTools storage) never sees a token.
 * The refresh cookie is only sent to /api/v1/auth, the access cookie to the whole API.
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";
    static final String ACCESS_PATH = "/";
    static final String REFRESH_PATH = "/api/v1/auth";

    private final boolean secure;
    private final Clock clock;

    public AuthCookies(@Value("${app.auth.cookie.secure}") boolean secure, Clock clock) {
        this.secure = secure;
        this.clock = clock;
    }

    /** Both Set-Cookie headers for a fresh session. */
    public HttpHeaders issue(AuthSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                build(ACCESS, session.accessToken().accessToken(), ACCESS_PATH, session.accessToken().expiresAt()).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                build(REFRESH, session.refreshToken(), REFRESH_PATH, session.refreshExpiresAt()).toString());
        return headers;
    }

    /** Both cookies expired, so the browser drops them. */
    public HttpHeaders clear() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, expired(ACCESS, ACCESS_PATH).toString());
        headers.add(HttpHeaders.SET_COOKIE, expired(REFRESH, REFRESH_PATH).toString());
        return headers;
    }

    private ResponseCookie build(String name, String value, String path, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        return base(name, value, path).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    private ResponseCookie expired(String name, String path) {
        return base(name, "", path).maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path);
    }
}
