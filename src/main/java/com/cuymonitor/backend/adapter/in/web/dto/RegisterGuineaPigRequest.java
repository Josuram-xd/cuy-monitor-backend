package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CoatColor;
import com.cuymonitor.backend.domain.model.GuineaPigBreed;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.MarkColor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// breed, coatColor, initialWeightGrams and notes are optional
public record RegisterGuineaPigRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull MarkColor markColor,
        GuineaPigBreed breed,
        CoatColor coatColor,
        @Min(GuineaPigProfile.MIN_WEIGHT_GRAMS) @Max(GuineaPigProfile.MAX_WEIGHT_GRAMS) Integer initialWeightGrams,
        @Size(max = GuineaPigProfile.MAX_NOTES_LENGTH) String notes) {

    public GuineaPigProfile profile() {
        return new GuineaPigProfile(breed, coatColor, initialWeightGrams, notes);
    }
}
