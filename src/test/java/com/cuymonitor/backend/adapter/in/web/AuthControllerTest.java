package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.exception.InvalidOtpException;
import com.cuymonitor.backend.domain.exception.UserAlreadyExistsException;
import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.auth.LoginChallenge;
import com.cuymonitor.backend.domain.port.in.LoginUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterUserCommand;
import com.cuymonitor.backend.domain.port.in.RegisterUserUseCase;
import com.cuymonitor.backend.domain.port.in.VerifyOtpUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class AuthControllerTest {

    private static final UUID CHALLENGE_ID = UUID.fromString("7d1c1f0e-0000-4000-8000-000000000001");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-01T10:05:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;
    @MockitoBean
    private LoginUseCase loginUseCase;
    @MockitoBean
    private VerifyOtpUseCase verifyOtpUseCase;

    @Test
    void registerReturns201WithTheChallenge() throws Exception {
        given(registerUserUseCase.register(argThat(matchesRegister("juan", "juan@mail.com"))))
                .willReturn(new LoginChallenge(CHALLENGE_ID, EXPIRES_AT));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","fullName":"Juan Perez","email":"juan@mail.com","password":"secret-pass"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challengeId").value(CHALLENGE_ID.toString()))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-01T10:05:00Z"));
    }

    @Test
    void registerWithInvalidBodyReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"","fullName":"Juan","email":"not-an-email","password":"secret-pass"}
                        """))
                .andExpect(status().isBadRequest())
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
                .andExpect(jsonPath("$.error").value("password too short"));
    }

    @Test
    void registerWithTakenUsernameReturns409() throws Exception {
        given(registerUserUseCase.register(any())).willThrow(new UserAlreadyExistsException());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","fullName":"Juan","email":"juan@mail.com","password":"secret-pass"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTheChallenge() throws Exception {
        given(loginUseCase.login(any())).willReturn(new LoginChallenge(CHALLENGE_ID, EXPIRES_AT));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"juan","password":"secret-pass"}
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
                .andExpect(jsonPath("$.error").value("invalid credentials"));
    }

    @Test
    void verifyOtpReturnsBearerToken() throws Exception {
        given(verifyOtpUseCase.verify(any())).willReturn(new AuthToken("jwt-token", EXPIRES_AT));

        mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"challengeId":"%s","code":"123456"}
                        """.formatted(CHALLENGE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
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
}
