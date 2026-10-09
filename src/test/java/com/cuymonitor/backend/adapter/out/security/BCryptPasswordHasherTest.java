package com.cuymonitor.backend.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashIsNotThePlainPasswordAndMatchesIt() {
        String hash = hasher.hashPassword("Secret-pass-1");

        assertThat(hash).isNotEqualTo("Secret-pass-1").startsWith("$2");
        assertThat(hasher.matches("Secret-pass-1", hash)).isTrue();
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = hasher.hashPassword("Secret-pass-1");

        assertThat(hasher.matches("other-pass", hash)).isFalse();
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfTheSalt() {
        assertThat(hasher.hashPassword("Secret-pass-1")).isNotEqualTo(hasher.hashPassword("Secret-pass-1"));
    }
}
