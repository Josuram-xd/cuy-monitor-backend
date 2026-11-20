package com.cuymonitor.backend.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Checks the X-API-Key of the machine endpoints (ai-service, serial bridge) in constant time. */
final class ApiKeyCheck {

    private final byte[] apiKey;

    ApiKeyCheck(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("app.api-key is required (set APP_API_KEY)");
        }
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    boolean matches(String providedKey) {
        return providedKey != null
                && MessageDigest.isEqual(apiKey, providedKey.getBytes(StandardCharsets.UTF_8));
    }
}
