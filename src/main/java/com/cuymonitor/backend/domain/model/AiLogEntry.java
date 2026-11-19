package com.cuymonitor.backend.domain.model;

import java.time.Instant;

/**
 * One line of the admin's AI log: a window the AI service analysed (BEHAVIOR), a sound it classified (AUDIO), or
 * what the assistant wrote about an alert (NOTE). The detail is in Spanish, like the reasons of the history.
 */
public record AiLogEntry(Instant at, String cageCode, Kind kind, String detail) {

    public enum Kind { BEHAVIOR, AUDIO, NOTE }
}
