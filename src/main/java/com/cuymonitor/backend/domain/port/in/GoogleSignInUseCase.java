package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.auth.AuthSession;

public interface GoogleSignInUseCase {
    /** Creates the account on the first visit, then starts a session. Google already proved the email. */
    AuthSession signIn(String idToken);
}
