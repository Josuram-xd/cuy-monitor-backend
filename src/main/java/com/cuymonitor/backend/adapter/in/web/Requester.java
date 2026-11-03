package com.cuymonitor.backend.adapter.in.web;

import java.security.Principal;
import java.util.UUID;

/**
 * Who is making the request: the account id in the subject of the session's JWT. One place for the controllers
 * (GRASP Pure Fabrication) instead of each one parsing the principal. It always comes from the session, never from
 * the request body or the URL, so nobody can act as another account.
 */
final class Requester {

    private Requester() {
    }

    static UUID idOf(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
