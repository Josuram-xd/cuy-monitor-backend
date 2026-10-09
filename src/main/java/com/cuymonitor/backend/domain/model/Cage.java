package com.cuymonitor.backend.domain.model;

import java.util.Objects;

// code is the public id ("cage-1") used in events, URLs and WebSocket topics
public record Cage(Long id, String code, String name) {

    public Cage {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
    }
}
