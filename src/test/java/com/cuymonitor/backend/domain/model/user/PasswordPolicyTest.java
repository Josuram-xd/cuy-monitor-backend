package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordWithinLimits() {
        assertThatCode(() -> PasswordPolicy.validate("12345678")).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validate("a".repeat(72))).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null)).isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("   ")).isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate("1234567")).isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void rejectsPasswordLongerThan72Bytes() {
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(73))).isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void countsBytesNotCharacters() {
        // 37 x "ñ" = 37 chars but 74 bytes in UTF-8
        assertThatThrownBy(() -> PasswordPolicy.validate("ñ".repeat(37))).isInstanceOf(WeakPasswordException.class);
    }
}
