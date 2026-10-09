package com.cuymonitor.backend.domain.model;

/**
 * What the owner tells about the guinea pig when registering it. Everything is optional: the ones
 * registered before the profile existed have none. Not to be confused with the mark color, which is
 * what the camera uses to tell them apart.
 */
public record GuineaPigProfile(GuineaPigBreed breed, CoatColor coatColor, Integer initialWeightGrams, String notes) {

    public static final int MIN_WEIGHT_GRAMS = 50;
    public static final int MAX_WEIGHT_GRAMS = 2000;
    public static final int MAX_NOTES_LENGTH = 500;

    public static final GuineaPigProfile EMPTY = new GuineaPigProfile(null, null, null, null);

    public GuineaPigProfile {
        if (initialWeightGrams != null
                && (initialWeightGrams < MIN_WEIGHT_GRAMS || initialWeightGrams > MAX_WEIGHT_GRAMS)) {
            throw new IllegalArgumentException(
                    "initialWeightGrams must be between " + MIN_WEIGHT_GRAMS + " and " + MAX_WEIGHT_GRAMS);
        }
        if (notes != null) {
            notes = notes.trim();
            if (notes.isEmpty()) {
                notes = null;
            } else if (notes.length() > MAX_NOTES_LENGTH) {
                throw new IllegalArgumentException("notes must have at most " + MAX_NOTES_LENGTH + " characters");
            }
        }
    }
}
