package com.cuymonitor.backend.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Reads the access token from the HttpOnly cookie. The Authorization header still works for tools like curl.
 * /api/v1/auth/** ignores the token on purpose: login, refresh and logout have to work when the old
 * access cookie is already expired, otherwise the user could never get a new session.
 * /ws ignores it too: the WebSocket validates the token on the STOMP CONNECT frame, so a bad cookie gets a
 * STOMP ERROR frame there instead of an HTTP 401 on the handshake.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final String AUTH_PREFIX = "/api/v1/auth/";
    private static final String WS_PATH = "/ws";

    private final BearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith(AUTH_PREFIX) || path.equals(WS_PATH) || path.startsWith(WS_PATH + "/")) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthCookies.ACCESS.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return headerResolver.resolve(request);
    }
}
