package com.cuymonitor.backend.domain.model;

import com.cuymonitor.backend.application.fake.Photos;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuineaPigProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

    @Test
    void everythingIsOptional() {
        GuineaPigProfile profile = new GuineaPigProfile(null, null, null, null);

        assertThat(profile).isEqualTo(GuineaPigProfile.EMPTY);
    }

    @Test
    void notesAreTrimmedAndBlankOnesAreDropped() {
        assertThat(new GuineaPigProfile(null, null, null, "  muerde la verdura  ").notes())
                .isEqualTo("muerde la verdura");
        assertThat(new GuineaPigProfile(null, null, null, "   ").notes()).isNull();
    }

    @Test
    void refusesNotesThatAreTooLong() {
        assertThatThrownBy(() -> new GuineaPigProfile(null, null, null, "a".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new GuineaPigProfile(null, null, null, "a".repeat(500)).notes()).hasSize(500);
    }

    @Test
    void refusesAWeightOutsideWhatAGuineaPigCanWeigh() {
        assertThatThrownBy(() -> new GuineaPigProfile(null, null, 49, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GuineaPigProfile(null, null, 2001, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new GuineaPigProfile(null, null, 50, null).initialWeightGrams()).isEqualTo(50);
        assertThat(new GuineaPigProfile(null, null, 2000, null).initialWeightGrams()).isEqualTo(2000);
    }

    @Test
    void aGuineaPigRegisteredWithoutProfileHasAnEmptyOne() {
        GuineaPig pig = GuineaPig.register("cage-1", "RED", Photos.png(), NOW);

        assertThat(pig.getProfile()).isEqualTo(GuineaPigProfile.EMPTY);
    }

    @Test
    void aGuineaPigKeepsTheProfileItWasRegisteredWith() {
        GuineaPigProfile profile =
                new GuineaPigProfile("TEDDY", "CREAM", 850, "tranquila");

        GuineaPig pig = GuineaPig.register("cage-1", "RED", Photos.png(), profile, NOW);

        assertThat(pig.getProfile()).isEqualTo(profile);
    }
}
