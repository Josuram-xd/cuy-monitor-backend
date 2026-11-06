package com.cuymonitor.backend.domain.exception;

/** The account asked the help assistant too many questions in a short time (each one costs a model call). */
public class TooManyAssistantQuestionsException extends RuntimeException {

    public TooManyAssistantQuestionsException() {
        super("too many questions to the assistant, try again in a few minutes");
    }
}
