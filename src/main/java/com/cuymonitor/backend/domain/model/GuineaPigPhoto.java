package com.cuymonitor.backend.domain.model;

import java.util.Arrays;
import java.util.Set;

/**
 * The picture the breeder recognizes a cuy by. It replaces the name, so it is required when registering.
 * The dashboard shrinks the picture before sending it; these limits are the last word.
 */
public final class GuineaPigPhoto {

    public static final int MAX_BYTES = ImageRules.MAX_BYTES;
    public static final Set<String> ALLOWED_CONTENT_TYPES = ImageRules.ALLOWED_CONTENT_TYPES;

    private final byte[] content;
    private final String contentType;

    public GuineaPigPhoto(byte[] content, String contentType) {
        ImageRules.check(content, contentType, "photo");
        this.content = content.clone();
        this.contentType = contentType;
    }

    public byte[] content() {
        return content.clone();
    }

    public String contentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GuineaPigPhoto photo
                && contentType.equals(photo.contentType)
                && Arrays.equals(content, photo.content);
    }

    @Override
    public int hashCode() {
        return 31 * contentType.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "GuineaPigPhoto[" + contentType + ", " + content.length + " bytes]";
    }
}
