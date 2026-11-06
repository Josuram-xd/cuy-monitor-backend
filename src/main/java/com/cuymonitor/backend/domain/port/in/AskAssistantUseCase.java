package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.assistant.AssistantReply;

/** The help assistant of the dashboard: answers how to use it and explains the state of the breeder's cages. */
public interface AskAssistantUseCase {

    AssistantReply ask(AskAssistantCommand command);
}
