package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.access.CageOperations;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final CageOperations cageOperations;

    public WebSocketSecurityConfig(JwtDecoder jwtDecoder, CageOperations cageOperations) {
        this.jwtDecoder = jwtDecoder;
        this.cageOperations = cageOperations;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor(jwtDecoder, cageOperations));
    }
}
