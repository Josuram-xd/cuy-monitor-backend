package com.cuymonitor.backend.domain.model.assistant;

import java.util.Objects;

/** What the help assistant answered. */
public record AssistantReply(String text) {

    public AssistantReply {
        Objects.requireNonNull(text, "text");
    }
}
