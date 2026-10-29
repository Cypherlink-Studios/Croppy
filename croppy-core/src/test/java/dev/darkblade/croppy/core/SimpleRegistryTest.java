package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.core.registry.SimpleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRegistryTest {

    private record TestEntry(Key key, String name) implements Keyed {
        @Override
        public Key getKey() {
            return key;
        }
    }

    private SimpleRegistry<TestEntry> registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleRegistry<>();
    }

    @Test
    @DisplayName("Register entry and query by key")
    void testRegisterAndGet() {
        Key key = Key.croppy("wheat");
        TestEntry entry = new TestEntry(key, "Wheat Crop");

        registry.register(entry);

        assertTrue(registry.contains(key));
        assertEquals(1, registry.size());
        assertFalse(registry.isEmpty());

        Optional<TestEntry> retrieved = registry.get(key);
        assertTrue(retrieved.isPresent());
        assertEquals("Wheat Crop", retrieved.get().name());
    }

    @Test
    @DisplayName("Duplicate registration throws IllegalStateException")
    void testDuplicateRegistration() {
        Key key = Key.croppy("wheat");
        TestEntry entry1 = new TestEntry(key, "Wheat 1");
        TestEntry entry2 = new TestEntry(key, "Wheat 2");

        registry.register(entry1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registry.register(entry2));
        assertTrue(ex.getMessage().contains("Duplicate"));
        assertEquals("Wheat 1", registry.get(key).orElseThrow().name());
    }

    @Test
    @DisplayName("Query non-existent key returns empty Optional")
    void testQueryNonExistentKey() {
        Key key = Key.croppy("unknown");
        Optional<TestEntry> result = registry.get(key);
        assertTrue(result.isEmpty());
        assertFalse(registry.contains(key));
    }

    @Test
    @DisplayName("Values collection is unmodifiable")
    void testUnmodifiableValues() {
        Key key = Key.croppy("carrot");
        registry.register(new TestEntry(key, "Carrot"));

        assertThrows(UnsupportedOperationException.class, () -> {
            registry.values().clear();
        });
    }
}
