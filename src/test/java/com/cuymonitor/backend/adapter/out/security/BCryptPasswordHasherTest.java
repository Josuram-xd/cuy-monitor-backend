package com.cuymonitor.backend.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashIsNotThePlainPasswordAndMatchesIt() {
        String hash = hasher.hashPassword("secret-pass");

        assertThat(hash).isNotEqualTo("secret-pass").startsWith("$2");
        assertThat(hasher.matches("secret-pass", hash)).isTrue();
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = hasher.hashPassword("secret-pass");

        assertThat(hasher.matches("other-pass", hash)).isFalse();
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfTheSalt() {
        assertThat(hasher.hashPassword("secret-pass")).isNotEqualTo(hasher.hashPassword("secret-pass"));
    }
}
