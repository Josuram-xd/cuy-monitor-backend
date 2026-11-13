package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.model.ImageRules;

import java.util.Arrays;

/**
 * The breeder's own picture. Optional, unlike a cuy's: an account without one shows its initials, the way it
 * always did. Same limits as any stored image, see {@link ImageRules}.
 */
public final class ProfilePhoto {

    private final byte[] content;
    private final String contentType;

    public ProfilePhoto(byte[] content, String contentType) {
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
        return other instanceof ProfilePhoto photo
                && contentType.equals(photo.contentType)
                && Arrays.equals(content, photo.content);
    }

    @Override
    public int hashCode() {
        return 31 * contentType.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "ProfilePhoto[" + contentType + ", " + content.length + " bytes]";
    }
}
