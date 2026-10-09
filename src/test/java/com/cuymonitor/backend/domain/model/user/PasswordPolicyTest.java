package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.WeakPasswordException;
import com.cuymonitor.backend.domain.model.user.PasswordPolicy.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private static List<Rule> broken(String password) {
        return PasswordPolicy.violations(password, "juan", "juan@mail.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cuyes-felices-9", "Zx9$kLm2pQ", "Ñandú#Rápido7", "Aa1!aaaaaa", "Tr0ub4dor&3-xyz"})
    void acceptsAStrongPassword(String password) {
        assertThat(broken(password)).isEmpty();
        assertThatCode(() -> PasswordPolicy.validate(password, "juan", "juan@mail.com")).doesNotThrowAnyException();
    }

    @Test
    void requiresAtLeastTenCharacters() {
        assertThat(broken("Aa1!aaaaa")).containsExactly(Rule.MIN_LENGTH);
        assertThat(broken("Aa1!aaaaaa")).isEmpty();
    }

    @Test
    void allowsAtMostSixtyFourCharacters() {
        assertThat(broken("Aa1!" + "a".repeat(60))).isEmpty();
        assertThat(broken("Aa1!" + "a".repeat(61))).containsExactly(Rule.MAX_LENGTH);
    }

    @Test
    void countsBytesBecauseBcryptCutsAfter72() {
        // 40 characters, but "ñ" takes 2 bytes: 82 bytes would be silently truncated
        assertThat(broken("Aa1!" + "ñ".repeat(36))).contains(Rule.MAX_LENGTH);
    }

    @Test
    void needsEveryKindOfCharacter() {
        assertThat(broken("ALLUPPER-123456")).containsExactly(Rule.LOWERCASE);
        assertThat(broken("alllower-123456")).containsExactly(Rule.UPPERCASE);
        assertThat(broken("NoDigitsHere-aaa")).containsExactly(Rule.DIGIT);
        assertThat(broken("NoSpecial12345aa")).containsExactly(Rule.SPECIAL);
    }

    @Test
    void anUnderscoreOrAHyphenCountsAsSpecialButALetterWithAnAccentDoesNot() {
        assertThat(broken("Cuyes_felices1")).isEmpty();
        assertThat(broken("Cuyés felices1")).contains(Rule.SPECIAL);
    }

    @Test
    void refusesSpaces() {
        assertThat(broken("Cuyes felices-9")).containsExactly(Rule.NO_SPACES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Password123!", "PASSWORD-2026", "Qwerty_12345", "Contraseña#2026", "Admin-123456"})
    void refusesWellKnownPasswordsEvenWithDecoration(String password) {
        assertThat(broken(password)).contains(Rule.NOT_COMMON);
    }

    @Test
    void refusesAPasswordMadeOfTheUsersOwnName() {
        assertThat(PasswordPolicy.violations("Mi-Juan-2026!!", "juan", "x@mail.com")).containsExactly(Rule.NOT_PERSONAL);
        assertThat(PasswordPolicy.violations("Ana#Maria-2026", "pedro", "ana.maria@mail.com"))
                .contains(Rule.NOT_PERSONAL);
        // very short names are ignored: "al" would match too many passwords
        assertThat(PasswordPolicy.violations("Algo-largo-12", "al", "al@mail.com")).isEmpty();
    }

    @Test
    void reportsEveryBrokenRuleAtOnce() {
        assertThat(broken("xyz")).containsExactly(Rule.MIN_LENGTH, Rule.UPPERCASE, Rule.DIGIT, Rule.SPECIAL);
    }

    @Test
    void aMissingPasswordBreaksTheBasicRules() {
        assertThat(broken(null)).contains(Rule.MIN_LENGTH, Rule.UPPERCASE);
        assertThat(broken("")).contains(Rule.MIN_LENGTH);
    }

    @Test
    void theExceptionCarriesTheRuleCodes() {
        assertThatThrownBy(() -> PasswordPolicy.validate("xyz", "juan", "juan@mail.com"))
                .isInstanceOfSatisfying(WeakPasswordException.class,
                        e -> assertThat(e.getRules()).containsExactly("MIN_LENGTH", "UPPERCASE", "DIGIT", "SPECIAL"));
    }
}
