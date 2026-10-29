package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// breed, coatColor, initialWeightGrams and notes are optional.
// The three codes are checked against catalog_option in GuineaPigService; here only their shape.
public record RegisterGuineaPigRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = CatalogOption.MAX_CODE_LENGTH) String markColor,
        @Size(max = CatalogOption.MAX_CODE_LENGTH) String breed,
        @Size(max = CatalogOption.MAX_CODE_LENGTH) String coatColor,
        @Min(GuineaPigProfile.MIN_WEIGHT_GRAMS) @Max(GuineaPigProfile.MAX_WEIGHT_GRAMS) Integer initialWeightGrams,
        @Size(max = GuineaPigProfile.MAX_NOTES_LENGTH) String notes) {

    public GuineaPigProfile profile() {
        return new GuineaPigProfile(breed, coatColor, initialWeightGrams, notes);
    }
}
