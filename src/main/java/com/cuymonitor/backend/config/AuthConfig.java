package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.security.JwtTokenIssuer;
import com.cuymonitor.backend.application.AuthenticationService;
import com.cuymonitor.backend.application.SessionService;
import com.cuymonitor.backend.application.UserAccountService;
import com.cuymonitor.backend.domain.port.out.OtpChallengeRepository;
import com.cuymonitor.backend.domain.port.out.OtpSender;
import com.cuymonitor.backend.domain.port.out.PasswordHasher;
import com.cuymonitor.backend.domain.port.out.RefreshTokenRepository;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import com.cuymonitor.backend.domain.port.out.TokenIssuer;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;

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
