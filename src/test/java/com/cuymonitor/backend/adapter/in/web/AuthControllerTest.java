package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.InvalidOtpException;
import com.cuymonitor.backend.domain.exception.InvalidRefreshTokenException;
import com.cuymonitor.backend.domain.exception.UserAlreadyExistsException;
import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.auth.LoginChallenge;
import com.cuymonitor.backend.domain.port.in.LoginUseCase;
import com.cuymonitor.backend.domain.port.in.LogoutUseCase;
import com.cuymonitor.backend.domain.port.in.RefreshSessionUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterUserCommand;
import com.cuymonitor.backend.domain.port.in.RegisterUserUseCase;
import com.cuymonitor.backend.domain.port.in.VerifyOtpUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class, AuthCookies.class, AuthControllerTest.FixedClock.class})
class AuthControllerTest {

    private static final UUID CHALLENGE_ID = UUID.fromString("7d1c1f0e-0000-4000-8000-000000000001");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-01T10:05:00Z");
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;
    @MockitoBean
    private LoginUseCase loginUseCase;
    @MockitoBean
    private VerifyOtpUseCase verifyOtpUseCase;
    @MockitoBean
    private RefreshSessionUseCase refreshSessionUseCase;
    @MockitoBean
    private LogoutUseCase logoutUseCase;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void registerReturns201WithTheChallenge() throws Exception {
        given(registerUserUseCase.register(argThat(matchesRegister("juan", "juan@mail.com"))))
                .willReturn(new LoginChallenge(CHALLENGE_ID, EXPIRES_AT));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","fullName":"Juan Perez","email":"juan@mail.com","password":"Secret-pass-1"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challengeId").value(CHALLENGE_ID.toString()))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-01T10:05:00Z"));
    }

    @Test
    void registerWithInvalidBodyReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"","fullName":"Juan","email":"not-an-email","password":"Secret-pass-1"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.email").exists());
        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void registerWithWeakPasswordReturns400() throws Exception {
        given(registerUserUseCase.register(any())).willThrow(new WeakPasswordException("password too short"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","fullName":"Juan","email":"juan@mail.com","password":"short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("password too short"));
    }

    @Test
    void aWeakPasswordListsTheBrokenRulesForTheDashboard() throws Exception {
        given(registerUserUseCase.register(any())).willThrow(new WeakPasswordException(
                "password does not meet the requirements", java.util.List.of("MIN_LENGTH", "SPECIAL")));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","fullName":"Juan","email":"juan@mail.com","password":"Short1"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password does not meet the requirements"))
                .andExpect(jsonPath("$.fields.password").value("MIN_LENGTH,SPECIAL"));
    }

    @Test
    void registerWithTakenUsernameReturns409() throws Exception {
        given(registerUserUseCase.register(any())).willThrow(new UserAlreadyExistsException());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","fullName":"Juan","email":"juan@mail.com","password":"Secret-pass-1"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void loginReturnsTheChallenge() throws Exception {
        given(loginUseCase.login(any())).willReturn(new LoginChallenge(CHALLENGE_ID, EXPIRES_AT));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","password":"Secret-pass-1"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeId").value(CHALLENGE_ID.toString()));
    }

    @Test
    void loginWithBadCredentialsReturns401() throws Exception {
        given(loginUseCase.login(any())).willThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","password":"wrong-pass"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("invalid credentials"));
    }

    @Test
    void verifyOtpSetsTheSessionInHttpOnlyCookiesAndNoBody() throws Exception {
        given(verifyOtpUseCase.verify(any())).willReturn(session("jwt-token", "refresh-raw"));

        mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"challengeId":"%s","code":"123456"}
                        """.formatted(CHALLENGE_ID)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(cookie().value("access_token", "jwt-token"))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().secure("access_token", true))
                .andExpect(cookie().sameSite("access_token", "Strict"))
                .andExpect(cookie().path("access_token", "/"))
                .andExpect(cookie().maxAge("access_token", 900))
                .andExpect(cookie().value("refresh_token", "refresh-raw"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().sameSite("refresh_token", "Strict"))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"))
                .andExpect(cookie().maxAge("refresh_token", 604800));
    }

    @Test
    void refreshRotatesBothCookies() throws Exception {
        given(refreshSessionUseCase.refresh("old-refresh")).willReturn(session("new-jwt", "new-refresh"));

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("access_token", "new-jwt"))
                .andExpect(cookie().value("refresh_token", "new-refresh"));
    }

    @Test
    void refreshWithoutAValidCookieReturns401() throws Exception {
        given(refreshSessionUseCase.refresh(any())).willThrow(new InvalidRefreshTokenException());

        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("invalid or expired session"));
    }

    @Test
    void logoutRevokesTheTokensInTheCookiesAndClearsThem() throws Exception {
        UUID jti = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        String accessToken = accessToken(jti, sid);

        mvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("access_token", accessToken), new Cookie("refresh_token", "the-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"));

        verify(logoutUseCase).logout(argThat(command -> jti.equals(command.accessTokenId())
                && sid.equals(command.sessionId()) && command.accessTokenExpiresAt() != null && "the-refresh".equals(command.refreshToken())));
    }

    @Test
    void logoutStillWorksWithAnExpiredOrBrokenAccessToken() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("access_token", "not-a-jwt"), new Cookie("refresh_token", "the-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("access_token", 0));

        verify(logoutUseCase).logout(argThat(command -> command.accessTokenId() == null
                && "the-refresh".equals(command.refreshToken())));
    }

    @Test
    void authEndpointsIgnoreAStaleAccessCookie() throws Exception {
        given(loginUseCase.login(any())).willReturn(new LoginChallenge(CHALLENGE_ID, EXPIRES_AT));

        mvc.perform(post("/api/v1/auth/login").cookie(new Cookie("access_token", "expired-or-garbage"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"juan","password":"Secret-pass-1"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void verifyOtpWithWrongCodeReturns401() throws Exception {
        given(verifyOtpUseCase.verify(any())).willThrow(new InvalidOtpException());

        mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"challengeId":"%s","code":"000000"}
                        """.formatted(CHALLENGE_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtpWithMalformedCodeReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"challengeId":"%s","code":"12ab"}
                        """.formatted(CHALLENGE_ID)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(verifyOtpUseCase);
    }

    private static ArgumentMatcher<RegisterUserCommand> matchesRegister(String username, String email) {
        return command -> command.username().equals(username) && command.email().equals(email);
    }

    private static AuthSession session(String accessToken, String refreshToken) {
        return new AuthSession(new AuthToken(accessToken, NOW.plusSeconds(900)), refreshToken, NOW.plusSeconds(604_800));
    }

    private String accessToken(UUID jti, UUID sid) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cuy-monitor-backend")
                .subject(UUID.randomUUID().toString())
                .id(jti.toString())
                .claim("sid", sid.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
