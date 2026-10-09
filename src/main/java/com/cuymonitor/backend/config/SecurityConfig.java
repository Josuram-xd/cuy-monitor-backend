package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.in.web.CookieBearerTokenResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No CSRF token: the session cookies are SameSite=Strict, so a request that starts on another
                // site never carries them, and the dashboard is served from this same site (see ARCHITECTURE.md)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ingestion is not public: IngestionController checks the X-API-Key header itself
                        .requestMatchers("/api/v1/auth/**", "/api/v1/ingestion/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // the handshake can't carry the token; JwtChannelInterceptor checks it on STOMP CONNECT
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CookieBearerTokenResolver())
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint()))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new JsonAuthenticationEntryPoint()));
        return http.build();
    }
}
