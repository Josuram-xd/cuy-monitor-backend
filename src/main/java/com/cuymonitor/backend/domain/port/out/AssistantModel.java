package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;

import java.util.List;

/**
 * The language model behind the help assistant (Amazon Bedrock in production). The conversation starts with a
 * USER message and alternates roles.
 */
public interface AssistantModel {

    /** @throws AssistantUnavailableException when the model can't answer now (off, throttled, network...) */
    String reply(String instructions, List<ChatMessage> conversation);
}
