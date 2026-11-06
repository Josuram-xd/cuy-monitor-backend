package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.model.assistant.ChatRole;
import com.cuymonitor.backend.domain.port.in.AskAssistantCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** POST /api/v1/assistant/messages: the conversation shown in the dashboard, ending with the new question. */
public record AssistantRequest(
        @NotEmpty @Size(max = AskAssistantCommand.MAX_MESSAGES) List<@Valid @NotNull Message> messages,
        @Pattern(regexp = "es|en") String language) {

    public record Message(@NotNull ChatRole role,
                          @NotBlank @Size(max = ChatMessage.MAX_TEXT_LENGTH) String text) {
    }

    public AskAssistantCommand toCommand(UUID requesterId) {
        return new AskAssistantCommand(requesterId,
                messages.stream().map(m -> new ChatMessage(m.role(), m.text())).toList(), language);
    }
}
