package dev.darkblade.croppy.api.update;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SemanticVersionTest {

    @Test
    @DisplayName("Should parse standard semantic versions")
    void testParseStandard() {
        SemanticVersion v1 = SemanticVersion.parse("1.2.3");
        assertEquals(1, v1.major());
        assertEquals(2, v1.minor());
        assertEquals(3, v1.patch());
        assertFalse(v1.isPreRelease());
        assertEquals("1.2.3", v1.asString());

        SemanticVersion v2 = SemanticVersion.parse("v0.5.13");
        assertEquals(0, v2.major());
        assertEquals(5, v2.minor());
        assertEquals(13, v2.patch());
        assertFalse(v2.isPreRelease());
        assertEquals("0.5.13", v2.asString());

        SemanticVersion v3 = SemanticVersion.parse("V2.0");
        assertEquals(2, v3.major());
        assertEquals(0, v3.minor());
        assertEquals(0, v3.patch());
    }

    @Test
    @DisplayName("Should parse pre-release versions")
    void testParsePreRelease() {
        SemanticVersion v = SemanticVersion.parse("v1.0.0-beta.2");
        assertEquals(1, v.major());
        assertEquals(0, v.minor());
        assertEquals(0, v.patch());
        assertTrue(v.isPreRelease());
        assertEquals("beta.2", v.preRelease().orElseThrow());
        assertEquals("1.0.0-beta.2", v.asString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1", "1.2.3.4", "-1.0.0", "v.1.2"})
    @DisplayName("Should throw on invalid version strings")
    void testInvalidFormats(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse(invalid));
    }

    @Test
    @DisplayName("Should correctly compare major, minor, and patch numbers")
    void testComparisonNumbers() {
        SemanticVersion v100 = SemanticVersion.of(1, 0, 0);
        SemanticVersion v101 = SemanticVersion.of(1, 0, 1);
        SemanticVersion v110 = SemanticVersion.of(1, 1, 0);
        SemanticVersion v200 = SemanticVersion.of(2, 0, 0);

        assertTrue(v101.isNewerThan(v100));
        assertTrue(v110.isNewerThan(v101));
        assertTrue(v200.isNewerThan(v110));
        assertFalse(v100.isNewerThan(v100));
        assertEquals(0, v100.compareTo(v100));
    }

    @Test
    @DisplayName("Should correctly evaluate pre-release precedence according to SemVer 2.0.0")
    void testPreReleasePrecedence() {
        SemanticVersion release = SemanticVersion.parse("1.0.0");
        SemanticVersion rc = SemanticVersion.parse("1.0.0-rc.1");
        SemanticVersion beta2 = SemanticVersion.parse("1.0.0-beta.2");
        SemanticVersion beta1 = SemanticVersion.parse("1.0.0-beta.1");
        SemanticVersion alpha = SemanticVersion.parse("1.0.0-alpha");

        // 1.0.0 > 1.0.0-rc.1 > 1.0.0-beta.2 > 1.0.0-beta.1 > 1.0.0-alpha
        assertTrue(release.isNewerThan(rc));
        assertTrue(rc.isNewerThan(beta2));
        assertTrue(beta2.isNewerThan(beta1));
        assertTrue(beta1.isNewerThan(alpha));

        // But 1.0.1-alpha > 1.0.0
        SemanticVersion nextAlpha = SemanticVersion.parse("1.0.1-alpha");
        assertTrue(nextAlpha.isNewerThan(release));
    }
}
