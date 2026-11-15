package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final CageOperations cageOperations;
    private final UserRepository userRepository;

    public WebSocketSecurityConfig(JwtDecoder jwtDecoder, CageOperations cageOperations,
                                  UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.cageOperations = cageOperations;
        this.userRepository = userRepository;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor(jwtDecoder, new CageTopicGuard(cageOperations),
                id -> userRepository.findById(id).filter(User::canLogIn).isPresent()));
    }
}
