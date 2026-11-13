package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.adapter.in.web.InvalidRequestException;
import com.cuymonitor.backend.domain.model.user.ProfilePhoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Base64;

/** The same shape a cuy's photo arrives in: base64 plus its type. */
public record ProfilePhotoRequest(
        // base64 of the image: 300 KB of bytes are about 410 000 characters
        @NotBlank @Size(max = 420_000) String photo,
        @NotBlank @Pattern(regexp = "image/(jpeg|png|webp)") String photoContentType) {

    /** Not named photo(): that is the record's own accessor for the base64 string. */
    public ProfilePhoto toPhoto() {
        byte[] content;
        try {
            content = Base64.getDecoder().decode(photo);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("photo is not valid base64");
        }
        try {
            return new ProfilePhoto(content, photoContentType);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
