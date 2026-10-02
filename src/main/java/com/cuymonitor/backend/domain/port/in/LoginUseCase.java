package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.auth.LoginChallenge;

public interface LoginUseCase {
    LoginChallenge login(LoginCommand loginCommand);
}
