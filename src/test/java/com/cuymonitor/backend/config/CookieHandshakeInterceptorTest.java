package com.cuymonitor.backend.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CookieHandshakeInterceptorTest {

    private final CookieHandshakeInterceptor interceptor = new CookieHandshakeInterceptor();

    @Test
    void copiesTheAccessCookieToTheSessionAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setCookies(new Cookie("access_token", "the-jwt"), new Cookie("other", "x"));
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed = interceptor.beforeHandshake(new ServletServerHttpRequest(request), null, null, attributes);

        assertThat(proceed).isTrue();
        assertThat(attributes).containsEntry("accessToken", "the-jwt");
    }

    @Test
    void withoutTheCookieItStillLetsTheHandshakeThrough() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed = interceptor.beforeHandshake(new ServletServerHttpRequest(request), null, null, attributes);

        // the CONNECT frame is the one that answers with an ERROR
        assertThat(proceed).isTrue();
        assertThat(attributes).isEmpty();
    }
}
