package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.in.web.AuthCookies;
import jakarta.servlet.http.Cookie;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * The browser sends the access cookie on the WebSocket handshake. We copy it to the session attributes,
 * where JwtChannelInterceptor picks it up on the STOMP CONNECT frame and validates it like any other token.
 */
public class CookieHandshakeInterceptor implements HandshakeInterceptor {

    static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (AuthCookies.ACCESS.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                        attributes.put(ACCESS_TOKEN_ATTRIBUTE, cookie.getValue());
                    }
                }
            }
        }
        // never reject here: a missing token is answered with an ERROR frame on CONNECT
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                               Exception exception) {
    }
}
