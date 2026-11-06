package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.AssistantRequest;
import com.cuymonitor.backend.adapter.in.web.dto.AssistantResponse;
import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.exception.TooManyAssistantQuestionsException;
import com.cuymonitor.backend.domain.port.in.AskAssistantCommand;
import com.cuymonitor.backend.domain.port.in.AskAssistantUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/** The help assistant of the dashboard. The account comes from the session, never from the body. */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AskAssistantUseCase askAssistant;

    public AssistantController(AskAssistantUseCase askAssistant) {
        this.askAssistant = askAssistant;
    }

    @PostMapping("/messages")
    public AssistantResponse ask(Principal principal, @Valid @RequestBody AssistantRequest request) {
        AskAssistantCommand command;
        try {
            command = request.toCommand(Requester.idOf(principal));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
        return AssistantResponse.from(askAssistant.ask(command));
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(AssistantUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.body(ApiError.SERVICE_UNAVAILABLE, "the assistant is not available right now"));
    }

    @ExceptionHandler(TooManyAssistantQuestionsException.class)
    public ResponseEntity<Map<String, Object>> handleTooMany(TooManyAssistantQuestionsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiError.body(ApiError.TOO_MANY_REQUESTS, ex.getMessage()));
    }
}
