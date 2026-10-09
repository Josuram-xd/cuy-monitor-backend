package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.user.User;

import java.util.UUID;

public interface TokenIssuer {
    /** sessionId ties every access token born from one login (it is the refresh family), so a logout can end them all. */
    AuthToken issueToken(User user, UUID sessionId);
}
