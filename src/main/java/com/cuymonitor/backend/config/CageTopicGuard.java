package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.UUID;

/**
 * Decides whether a STOMP subscription may follow a cage topic ({@code /topic/cages/{cageCode}}). It asks the cage
 * access proxy, the same one the REST API goes through, so the rule "only the owner sees a cage" lives in one place.
 * Other destinations are not its business and pass.
 */
public class CageTopicGuard {

    static final String CAGE_TOPIC = "/topic/cages/";

    private final CageOperations cageOperations;

    public CageTopicGuard(CageOperations cageOperations) {
        this.cageOperations = cageOperations;
    }

    /** @throws AccessDeniedException if the destination is a cage the user does not own (or that does not exist) */
    public void check(Principal user, String destination) {
        if (destination == null || !destination.startsWith(CAGE_TOPIC)) {
            return;
        }
        String cageCode = destination.substring(CAGE_TOPIC.length());
        try {
            cageOperations.getCage(UUID.fromString(user.getName()), cageCode);
        } catch (CageAccessDeniedException | CageNotFoundException | IllegalArgumentException e) {
            throw new AccessDeniedException("not allowed to follow cage " + cageCode, e);
        }
    }
}
