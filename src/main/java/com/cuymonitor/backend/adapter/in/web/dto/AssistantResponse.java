package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.assistant.AssistantReply;

public record AssistantResponse(String reply) {

    public static AssistantResponse from(AssistantReply reply) {
        return new AssistantResponse(reply.text());
    }
}
