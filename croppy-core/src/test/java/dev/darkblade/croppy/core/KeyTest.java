package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyTest {

    @Test
    @DisplayName("Valid key creation and formatting")
    void testValidKeyCreation() {
        Key key = Key.of("croppy", "wheat");
        assertEquals("croppy", key.namespace());
        assertEquals("wheat", key.value());
        assertEquals("croppy:wheat", key.asString());
        assertEquals("croppy:wheat", key.toString());

        Key identical = new Key("croppy", "wheat");
        assertEquals(key, identical);
        assertEquals(key.hashCode(), identical.hashCode());
    }

    @Test
    @DisplayName("Key normalization to lowercase")
    void testKeyNormalization() {
        Key key = Key.of("CROPPY", "WHEAT");
        assertEquals("croppy", key.namespace());
        assertEquals("wheat", key.value());
        assertEquals("croppy:wheat", key.asString());
    }

    @Test
    @DisplayName("Invalid key throws IllegalArgumentException")
    void testInvalidKeyComponents() {
        assertThrows(IllegalArgumentException.class, () -> Key.of(null, "wheat"));
        assertThrows(IllegalArgumentException.class, () -> Key.of("croppy", null));
        assertThrows(IllegalArgumentException.class, () -> Key.of("", "wheat"));
        assertThrows(IllegalArgumentException.class, () -> Key.of("croppy", "   "));
        assertThrows(IllegalArgumentException.class, () -> Key.of("croppy", "invalid/char"));
        assertThrows(IllegalArgumentException.class, () -> Key.of("invalid space", "wheat"));
    }

    @Test
    @DisplayName("Key parsing from string format")
    void testKeyParsing() {
        Key parsedWithNamespace = Key.parse("custom:tomato");
        assertEquals("custom", parsedWithNamespace.namespace());
        assertEquals("tomato", parsedWithNamespace.value());

        Key parsedDefault = Key.parse("potato");
        assertEquals(Key.DEFAULT_NAMESPACE, parsedDefault.namespace());
        assertEquals("potato", parsedDefault.value());
    }
}
