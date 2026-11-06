package com.cuymonitor.backend.adapter.out.assistant;

import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.port.out.AssistantModel;

import java.util.List;

/** The assistant when app.assistant.enabled is false (local development, tests): it always says it is off. */
public class DisabledAssistantModel implements AssistantModel {

    @Override
    public String reply(String instructions, List<ChatMessage> conversation) {
        throw new AssistantUnavailableException("the assistant is turned off");
    }
}
