package com.cuymonitor.backend.domain.model.assistant;

import java.util.Objects;

/** One message of a conversation with the help assistant, as the breeder or the assistant wrote it. */
public record ChatMessage(ChatRole role, String text) {

    public static final int MAX_TEXT_LENGTH = 1000;

    public ChatMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
        text = text.strip();
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("a message must have 1 to " + MAX_TEXT_LENGTH + " characters");
        }
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(ChatRole.USER, text);
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(ChatRole.ASSISTANT, text);
    }
}
