package com.openclassrooms.mdd_api.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OcAppProperties}.
 */
class OcAppPropertiesTest {

    @Test
    @DisplayName("equals/hashCode: contracts + main branches (Sonar-friendly)")
    void equalsHashCode_contracts() {
        // Arrange
        OcAppProperties a = props("secret", 1000L, 2000L, true);
        OcAppProperties b = props("secret", 1000L, 2000L, true);
        OcAppProperties different = props("other", 1000L, 2000L, true);

        // Act + Assert (avoid 'isEqualTo called on itself' and 'incompatible types' warnings)
        assertThat(a)
                .isEqualTo(a)                 // self
                .isNotEqualTo(null)     // null
                .isNotEqualTo("nope")   // different type
                .isEqualTo(b)
                .hasSameHashCodeAs(b)
                .isNotEqualTo(different);
    }

    @Test
    @DisplayName("equals: covers canEqual(false) branch")
    void equals_canEqualFalseBranch() {
        // Arrange
        OcAppProperties base = props("secret", 1000L, 2000L, false);

        OcAppProperties other = new NoEqualProps();
        other.setJwtSecret("secret");
        other.setJwtExpirationMs(1000L);
        other.setRefreshTokenExpirationMs(2000L);
        other.setCookieSecure(false);

        // Act + Assert
        assertThat(base.equals(other)).isFalse();
    }

    @Test
    @DisplayName("toString: contains key field names (avoid strict format)")
    void toString_containsUsefulInfo() {
        // Arrange
        OcAppProperties p = props("secret", 1000L, 2000L, false);

        // Act
        String s = p.toString();

        // Assert (Sonar-friendly: no direct toString call inside assertion)
        assertThat(p).hasToString(s);
        assertThat(s).contains(
                "OcAppProperties",
                "jwtSecret",
                "jwtExpirationMs",
                "refreshTokenExpirationMs",
                "cookieSecure"
        );
    }

    private static OcAppProperties props(String secret, long jwtExp, long refreshExp, boolean secure) {
        OcAppProperties p = new OcAppProperties();
        p.setJwtSecret(secret);
        p.setJwtExpirationMs(jwtExp);
        p.setRefreshTokenExpirationMs(refreshExp);
        p.setCookieSecure(secure);
        return p;
    }

    private static final class NoEqualProps extends OcAppProperties {
        @Override
        public boolean canEqual(Object other) {
            return false;
        }
    }
}
