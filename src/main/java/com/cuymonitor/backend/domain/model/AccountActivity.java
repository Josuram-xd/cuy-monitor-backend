package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * What an admin sees about an account's use of the system: counts only, never what is inside its cages.
 *
 * @param lastActivityAt the newest event received for any of its cages; null when none ever arrived
 */
public record AccountActivity(UUID userId, int cages, int guineaPigs, int openAlerts, Instant lastActivityAt) {

    public static AccountActivity none(UUID userId) {
        return new AccountActivity(userId, 0, 0, 0, null);
    }
}
