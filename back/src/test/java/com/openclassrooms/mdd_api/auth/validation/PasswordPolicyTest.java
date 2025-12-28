package com.openclassrooms.mdd_api.auth.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PasswordPolicy}.
 * SUT:
 *  - PasswordPolicy#isValid(String)
 * Covered rules:
 *  - not null
 *  - length >= 8
 *  - contains at least one: lower, upper, digit, special (non letter/digit)
 */
class PasswordPolicyTest {

    @Test
    @DisplayName("isValid: returns false when password is null")
    void isValid_returnsFalse_whenNull() {
        // Arrange
        String password = null;

        // Act
        boolean result = PasswordPolicy.isValid(password);

        // Assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "isValid: returns false for invalid password \"{0}\"")
    @MethodSource("invalidPasswords")
    void isValid_returnsFalse_forInvalidPasswords(String password) {
        // Arrange

        // Act
        boolean result = PasswordPolicy.isValid(password);

        // Assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "isValid: returns true for valid password \"{0}\"")
    @MethodSource("validPasswords")
    void isValid_returnsTrue_forValidPasswords(String password) {
        // Arrange

        // Act
        boolean result = PasswordPolicy.isValid(password);

        // Assert
        assertThat(result).isTrue();
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(
                "",                 // empty
                "Aa1!aaa",          // 7 chars (too short)
                "abcdefgh!",        // no upper, no digit
                "ABCDEFGH1!",       // no lower
                "Abcdefgh!",        // no digit
                "Abcdefg1",         // no special
                "Abcdef12",         // no special (letters+digits only)
                "Abcdef1é"          // 'é' is a letter -> still no special
        );
    }

    static Stream<String> validPasswords() {
        return Stream.of(
                "Abcdef1!",          // minimal valid (8 chars)
                "Abcdef1_",          // underscore counts as special
                "Abcdef1 ",          // space counts as special
                "Zz9@xxxx",          // another 8 chars
                "Longer1!Password"   // longer valid
        );
    }
}
