package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.auth.GoogleIdentity;

public interface GoogleIdTokenVerifier {
    /** @throws com.cuymonitor.backend.domain.exception.InvalidGoogleTokenException if it cannot be trusted */
    GoogleIdentity verify(String idToken);
}
