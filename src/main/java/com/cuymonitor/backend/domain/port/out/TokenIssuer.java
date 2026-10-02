package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.user.User;

public interface TokenIssuer {
    AuthToken issueToken(User user);
}
