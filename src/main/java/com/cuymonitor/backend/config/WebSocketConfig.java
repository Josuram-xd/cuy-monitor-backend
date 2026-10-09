package com.cuymonitor.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Empty in production: same origin only. The dev profile adds the Vite dev server, whose Origin
    // (localhost:5173) differs from the backend's host when the browser goes through the dev proxy.
    private final String[] allowedOrigins;

    public WebSocketConfig(@Value("${app.ws.allowed-origins:}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // plain WebSocket, no SockJS; the dashboard is served from the same origin behind Caddy
        var endpoint = registry.addEndpoint("/ws").addInterceptors(new CookieHandshakeInterceptor());
        if (allowedOrigins.length > 0) {
            endpoint.setAllowedOrigins(allowedOrigins);
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // server -> client only: alerts and status changes go to /topic/cages/{cageCode}
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
