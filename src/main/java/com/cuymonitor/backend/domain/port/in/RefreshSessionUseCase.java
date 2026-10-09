package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.auth.AuthSession;

public interface RefreshSessionUseCase {
    AuthSession refresh(String refreshToken);
}
