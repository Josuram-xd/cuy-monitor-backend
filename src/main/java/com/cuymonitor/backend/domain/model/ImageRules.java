package com.cuymonitor.backend.domain.model;

import java.util.Set;

/**
 * The limits every picture stored in the database has to meet. One place, because they also live as CHECK
 * constraints in cuy-monitor-db (V11 for a cuy, V17 for an account) and the two have to agree: if they drift,
 * the application accepts an image the database then refuses.
 */
public final class ImageRules {

    /** 300 KB. The dashboard shrinks the picture before sending; this is the last word. */
    public static final int MAX_BYTES = 307_200;

    /** Only formats a browser cannot be tricked into running: no SVG, the bytes are served back to it. */
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private ImageRules() {
    }

    /** @param what how to name the picture in the message, e.g. "photo". */
    public static void check(byte[] content, String contentType, String what) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException(what + " is required");
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException(what + " must be at most " + MAX_BYTES + " bytes");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(what + " must be one of " + ALLOWED_CONTENT_TYPES);
        }
    }
}
