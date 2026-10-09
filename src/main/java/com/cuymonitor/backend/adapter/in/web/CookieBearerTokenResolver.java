package com.cuymonitor.backend.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Reads the access token from the HttpOnly cookie. The Authorization header still works for tools like curl.
 * /api/v1/auth/** ignores the token on purpose: login, refresh and logout have to work when the old
 * access cookie is already expired, otherwise the user could never get a new session.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final BearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getRequestURI().startsWith(AUTH_PREFIX)) {
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
