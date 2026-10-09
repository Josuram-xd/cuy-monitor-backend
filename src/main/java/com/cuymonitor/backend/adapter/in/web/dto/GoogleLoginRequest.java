package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** idToken is the "credential" Google's button hands to the page. A real one is about 1 KB. */
public record GoogleLoginRequest(@NotBlank @Size(max = 4096) String idToken) {

    @Override
    public String toString() {
        return "GoogleLoginRequest[idToken=***]";
    }
}
