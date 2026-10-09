package com.cuymonitor.backend.adapter.in.web;

import java.util.LinkedHashMap;
import java.util.Map;

// error body shared by every endpoint: { "error": "<code>", "message": "..." } (see docs/contracts/rest-api.md)
public final class ApiError {

    public static final String BAD_REQUEST = "bad_request";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String NOT_FOUND = "not_found";
    public static final String CONFLICT = "conflict";
    public static final String TOO_MANY_REQUESTS = "too_many_requests";

    private ApiError() {
    }

    public static Map<String, Object> body(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}
