package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.auth.AuthToken;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.port.out.TokenIssuer;

import java.time.Instant;

public class FakeTokenIssuer implements TokenIssuer {

    @Override
    public AuthToken issueToken(User user) {
        return new AuthToken("token-for-" + user.getId(), Instant.parse("2100-01-01T00:00:00Z"));
    }
}
