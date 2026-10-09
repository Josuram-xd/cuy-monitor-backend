package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.ChallengeResponse;
import com.cuymonitor.backend.adapter.in.web.dto.LoginRequest;
import com.cuymonitor.backend.adapter.in.web.dto.RegisterRequest;
import com.cuymonitor.backend.adapter.in.web.dto.VerifyOtpRequest;
import com.cuymonitor.backend.domain.model.auth.AuthSession;
import com.cuymonitor.backend.domain.port.in.LoginCommand;
import com.cuymonitor.backend.domain.port.in.LoginUseCase;
import com.cuymonitor.backend.domain.port.in.LogoutCommand;
import com.cuymonitor.backend.domain.port.in.LogoutUseCase;
import com.cuymonitor.backend.domain.port.in.RefreshSessionUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterUserCommand;
import com.cuymonitor.backend.domain.port.in.RegisterUserUseCase;
import com.cuymonitor.backend.domain.port.in.VerifyOtpCommand;
import com.cuymonitor.backend.domain.port.in.VerifyOtpUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCookies cookies;
    private final JwtDecoder jwtDecoder;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase,
                          VerifyOtpUseCase verifyOtpUseCase, RefreshSessionUseCase refreshSessionUseCase,
                          LogoutUseCase logoutUseCase, AuthCookies cookies, JwtDecoder jwtDecoder) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
        this.logoutUseCase = logoutUseCase;
        this.cookies = cookies;
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeResponse register(@Valid @RequestBody RegisterRequest request) {
        return ChallengeResponse.from(registerUserUseCase.register(
                new RegisterUserCommand(request.username(), request.fullName(), request.email(), request.password())));
    }

    @PostMapping("/login")
    public ChallengeResponse login(@Valid @RequestBody LoginRequest request) {
        return ChallengeResponse.from(loginUseCase.login(new LoginCommand(request.username(), request.password())));
    }

    /** The tokens travel only in cookies; the body is empty. */
    @PostMapping("/otp/verify")
    public ResponseEntity<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthSession session = verifyOtpUseCase.verify(new VerifyOtpCommand(request.challengeId(), request.code()));
        return ResponseEntity.noContent().headers(cookies.issue(session)).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        AuthSession session = refreshSessionUseCase.refresh(refreshToken);
        return ResponseEntity.noContent().headers(cookies.issue(session)).build();
    }

    /** Always ends with the cookies cleared, even if the access token already expired or was never sent. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookies.ACCESS, required = false) String accessToken,
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        Jwt jwt = readLeniently(accessToken);
        UUID tokenId = jwt == null ? null : UUID.fromString(jwt.getId());
        logoutUseCase.logout(new LogoutCommand(tokenId, jwt == null ? null : jwt.getExpiresAt(), refreshToken));
        return ResponseEntity.noContent().headers(cookies.clear()).build();
    }

    // an expired, revoked or garbage token just means there is nothing to revoke
    private Jwt readLeniently(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            return jwtDecoder.decode(accessToken);
        } catch (JwtException e) {
            return null;
        }
    }
}
