package com.cuymonitor.backend.adapter.in.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

    @Test
    void readsTheAccessCookie() {
        MockHttpServletRequest request = request("/api/v1/account/profile");
        request.setCookies(new Cookie("access_token", "from-cookie"));

        assertThat(resolver.resolve(request)).isEqualTo("from-cookie");
    }

    @Test
    void theCookieWinsOverTheHeader() {
        MockHttpServletRequest request = request("/api/v1/account/profile");
        request.setCookies(new Cookie("access_token", "from-cookie"));
        request.addHeader("Authorization", "Bearer from-header");

        assertThat(resolver.resolve(request)).isEqualTo("from-cookie");
    }

    @Test
    void fallsBackToTheAuthorizationHeader() {
        MockHttpServletRequest request = request("/api/v1/account/profile");
        request.addHeader("Authorization", "Bearer from-header");

        assertThat(resolver.resolve(request)).isEqualTo("from-header");
    }

    @Test
    void ignoresBlankCookiesAndOtherCookies() {
        MockHttpServletRequest request = request("/api/v1/account/profile");
        request.setCookies(new Cookie("access_token", " "), new Cookie("refresh_token", "refresh"));

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void returnsNullWithoutAnyCredentials() {
        assertThat(resolver.resolve(request("/api/v1/account/profile"))).isNull();
    }

    @Test
    void authEndpointsNeverUseTheToken() {
        MockHttpServletRequest request = request("/api/v1/auth/refresh");
        request.setCookies(new Cookie("access_token", "expired"));
        request.addHeader("Authorization", "Bearer expired");

        assertThat(resolver.resolve(request)).isNull();
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }
}
