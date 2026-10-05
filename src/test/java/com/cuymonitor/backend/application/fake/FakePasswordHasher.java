package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.port.out.PasswordHasher;

public class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hashPassword(String rawPassword) {
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return hashedPassword.equals("hashed:" + rawPassword);
    }
}
