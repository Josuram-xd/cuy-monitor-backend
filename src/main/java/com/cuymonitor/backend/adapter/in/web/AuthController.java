package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.ChallengeResponse;
import com.cuymonitor.backend.adapter.in.web.dto.LoginRequest;
import com.cuymonitor.backend.adapter.in.web.dto.RegisterRequest;
import com.cuymonitor.backend.adapter.in.web.dto.TokenResponse;
import com.cuymonitor.backend.adapter.in.web.dto.VerifyOtpRequest;
import com.cuymonitor.backend.domain.port.in.LoginCommand;
import com.cuymonitor.backend.domain.port.in.LoginUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterUserCommand;
import com.cuymonitor.backend.domain.port.in.RegisterUserUseCase;
import com.cuymonitor.backend.domain.port.in.VerifyOtpCommand;
import com.cuymonitor.backend.domain.port.in.VerifyOtpUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase,
                          VerifyOtpUseCase verifyOtpUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
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

    @PostMapping("/otp/verify")
    public TokenResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return TokenResponse.from(verifyOtpUseCase.verify(new VerifyOtpCommand(request.challengeId(), request.code())));
    }
}
