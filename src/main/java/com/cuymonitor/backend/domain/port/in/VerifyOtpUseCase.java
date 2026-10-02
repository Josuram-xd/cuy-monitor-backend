package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.auth.AuthToken;

public interface VerifyOtpUseCase {
    AuthToken verify(VerifyOtpCommand verifyOtpCommand);
}
