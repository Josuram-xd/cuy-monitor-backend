package com.cuymonitor.backend.domain.model.user;

import com.cuymonitor.backend.domain.exception.WeakPasswordException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What a new password must look like: 10 to 64 characters with a lowercase letter, an uppercase letter,
 * a digit and a special character, no spaces, not a well-known password and not made of the user's own
 * name. The dashboard shows the same rules while the user types; this class is the one that decides.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 64;
    // BCrypt only reads the first 72 bytes: a longer password would be silently cut
    public static final int MAX_BYTES = 72;

    /** Each value is a rule a password can break; the names travel to the dashboard as codes. */
    public enum Rule {
        MIN_LENGTH, MAX_LENGTH, LOWERCASE, UPPERCASE, DIGIT, SPECIAL, NO_SPACES, NOT_COMMON, NOT_PERSONAL
    }

    // compared against the letters of the password only, so "Password123!" is caught too
    private static final Set<String> COMMON = Set.of(
            "password", "contrasena", "contraseña", "qwerty", "qwertyuiop", "asdfgh", "admin", "administrator",
            "welcome", "letmein", "iloveyou", "monkey", "dragon", "abc", "monitor", "cuymonitor", "cuy", "cuyes",
            "guineapig", "secret", "changeme", "test", "usuario", "clave");
    private static final int MIN_IDENTITY_LENGTH = 3;

    private PasswordPolicy() {
    }

    /** Validates a password for someone whose username and email are not known (rare). */
    public static void validate(String rawPassword) {
        validate(rawPassword, null, null);
    }

    public static void validate(String rawPassword, String username, String email) {
        List<Rule> broken = violations(rawPassword, username, email);
        if (!broken.isEmpty()) {
            throw new WeakPasswordException("password does not meet the requirements",
                    broken.stream().map(Enum::name).toList());
        }
    }

    public static List<Rule> violations(String rawPassword, String username, String email) {
        List<Rule> broken = new ArrayList<>();
        if (rawPassword == null || rawPassword.isEmpty()) {
            return List.of(Rule.MIN_LENGTH, Rule.LOWERCASE, Rule.UPPERCASE, Rule.DIGIT, Rule.SPECIAL);
        }
        int length = rawPassword.codePointCount(0, rawPassword.length());
        if (length < MIN_LENGTH) {
            broken.add(Rule.MIN_LENGTH);
        }
        if (length > MAX_LENGTH || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            broken.add(Rule.MAX_LENGTH);
        }
        if (rawPassword.codePoints().noneMatch(Character::isLowerCase)) {
            broken.add(Rule.LOWERCASE);
        }
        if (rawPassword.codePoints().noneMatch(Character::isUpperCase)) {
            broken.add(Rule.UPPERCASE);
        }
        if (rawPassword.codePoints().noneMatch(Character::isDigit)) {
            broken.add(Rule.DIGIT);
        }
        if (rawPassword.codePoints().noneMatch(PasswordPolicy::isSpecial)) {
            broken.add(Rule.SPECIAL);
        }
        if (rawPassword.codePoints().anyMatch(Character::isWhitespace)) {
            broken.add(Rule.NO_SPACES);
        }
        if (isCommon(rawPassword)) {
            broken.add(Rule.NOT_COMMON);
        }
        if (containsIdentity(rawPassword, username, email)) {
            broken.add(Rule.NOT_PERSONAL);
        }
        return broken;
    }

    // a symbol or punctuation mark: not a letter, not a digit, not a space
    private static boolean isSpecial(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }

    private static boolean isCommon(String rawPassword) {
        StringBuilder letters = new StringBuilder();
        rawPassword.toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetter)
                .forEach(letters::appendCodePoint);
        return COMMON.contains(letters.toString());
    }

    private static boolean containsIdentity(String rawPassword, String username, String email) {
        String lower = rawPassword.toLowerCase(Locale.ROOT);
        return mentions(lower, username) || mentions(lower, email == null ? null : email.split("@")[0]);
    }

    // "ana.maria" is two names: either one inside the password is already too personal
    private static boolean mentions(String lowerPassword, String identity) {
        if (identity == null) {
            return false;
        }
        for (String part : identity.trim().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (part.length() >= MIN_IDENTITY_LENGTH && lowerPassword.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
