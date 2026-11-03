package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.adapter.in.web.InvalidRequestException;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Base64;

// breed, coatColor, initialWeightGrams and notes are optional.
// The three codes are checked against catalog_option in GuineaPigService; here only their shape.
public record RegisterGuineaPigRequest(
        // base64 of the image: 300 KB of bytes are about 410 000 characters
        @NotBlank @Size(max = 420_000) String photo,
        @NotBlank @Pattern(regexp = "image/(jpeg|png|webp)") String photoContentType,
        @NotBlank @Size(max = CatalogOption.MAX_CODE_LENGTH) String markColor,
        @Size(max = CatalogOption.MAX_CODE_LENGTH) String breed,
        @Size(max = CatalogOption.MAX_CODE_LENGTH) String coatColor,
        @Min(GuineaPigProfile.MIN_WEIGHT_GRAMS) @Max(GuineaPigProfile.MAX_WEIGHT_GRAMS) Integer initialWeightGrams,
        @Size(max = GuineaPigProfile.MAX_NOTES_LENGTH) String notes) {

    /** Not named photo(): that is the record's own accessor for the base64 string. */
    public GuineaPigPhoto toPhoto() {
        byte[] content;
        try {
            content = Base64.getDecoder().decode(photo);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("photo is not valid base64");
        }
        try {
            return new GuineaPigPhoto(content, photoContentType);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    public GuineaPigProfile profile() {
        return new GuineaPigProfile(breed, coatColor, initialWeightGrams, notes);
    }
}
