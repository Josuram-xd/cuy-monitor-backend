package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.auth.AuthSession;

/** "Iniciar como usuario normal": opens a session in the public demo account, without password or code. */
public interface DemoSignInUseCase {

    AuthSession signIn();

    /** "Entrar como admin": the admin account named for the professor's review, no password or code. */
    AuthSession signInAsAdmin();
}
