package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.model.assistant.ChatRole;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A question for the help assistant. The conversation is sent whole by the dashboard (the backend keeps no chat
 * history), oldest first, and ends with the breeder's new question. requesterId comes from the session.
 *
 * @param language "es" or "en", the language the dashboard is shown in
 */
public record AskAssistantCommand(UUID requesterId, List<ChatMessage> conversation, String language) {

    public static final int MAX_MESSAGES = 12;

    public AskAssistantCommand {
        Objects.requireNonNull(requesterId, "requesterId");
        conversation = List.copyOf(Objects.requireNonNull(conversation, "conversation"));
        if (conversation.isEmpty() || conversation.size() > MAX_MESSAGES) {
            throw new IllegalArgumentException("a conversation has 1 to " + MAX_MESSAGES + " messages");
        }
        if (conversation.getLast().role() != ChatRole.USER) {
            throw new IllegalArgumentException("the last message must be the breeder's question");
        }
        language = "en".equals(language) ? "en" : "es";
    }
}
