package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.security.JwtTokenIssuer;
import com.cuymonitor.backend.adapter.out.security.GoogleIdTokenVerifierAdapter;
import com.cuymonitor.backend.application.AuthenticationService;
import com.cuymonitor.backend.application.GoogleSignInService;
import com.cuymonitor.backend.application.SessionService;
import com.cuymonitor.backend.application.UserAccountService;
import com.cuymonitor.backend.domain.port.out.OtpChallengeRepository;
import com.cuymonitor.backend.domain.port.out.OtpSender;
import com.cuymonitor.backend.domain.port.out.GoogleIdTokenVerifier;
import com.cuymonitor.backend.domain.port.out.PasswordHasher;
import com.cuymonitor.backend.domain.port.out.RefreshTokenRepository;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import com.cuymonitor.backend.domain.port.out.TokenIssuer;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Clock;

@Configuration
public class AuthConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TokenIssuer tokenIssuer(JwtEncoder jwtEncoder, AuthProperties properties, Clock clock) {
        return new JwtTokenIssuer(jwtEncoder, properties.jwt().issuer(), properties.jwt().ttl(), clock);
    }

    @Bean
    public SessionService sessionService(TokenIssuer tokenIssuer, RefreshTokenRepository refreshTokenRepository,
                                         RevokedTokenRepository revokedTokenRepository, UserRepository userRepository,
                                         Clock clock, AuthProperties properties) {
        return new SessionService(tokenIssuer, refreshTokenRepository, revokedTokenRepository, userRepository, clock,
                properties.refresh().ttl(), properties.jwt().ttl());
    }

    // Not a JwtDecoder bean on purpose: the API already has one for its own tokens and Spring injects it by type.
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(AuthProperties properties) {
        String clientId = properties.google() == null ? "" : properties.google().clientId();
        return new GoogleIdTokenVerifierAdapter(
                NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build(),
                clientId);
    }

    @Bean
    public GoogleSignInService googleSignInService(UserRepository userRepository, GoogleIdTokenVerifier verifier,
                                                   SessionService sessionService, Clock clock) {
        return new GoogleSignInService(userRepository, verifier, sessionService, clock);
    }

    @Bean
    public AuthenticationService authenticationService(UserRepository userRepository,
                                                       OtpChallengeRepository otpChallengeRepository,
                                                       PasswordHasher passwordHasher, OtpSender otpSender,
                                                       SessionService sessionService, Clock clock,
                                                       AuthProperties properties) {
        return new AuthenticationService(userRepository, otpChallengeRepository, passwordHasher, otpSender,
                sessionService, clock, properties.otp().ttl(), properties.otp().maxAttempts(),
                properties.otp().maxRequests(), properties.otp().requestWindow());
    }

    @Bean
    public UserAccountService userAccountService(UserRepository userRepository, PasswordHasher passwordHasher,
                                                 Clock clock) {
        return new UserAccountService(userRepository, passwordHasher, clock);
    }
}
