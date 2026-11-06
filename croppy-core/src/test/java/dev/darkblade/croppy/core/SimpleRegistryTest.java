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

    @Test
    @DisplayName("Unregister existing entry removes it and returns true")
    void testUnregisterExistingKey() {
        Key key = Key.croppy("potato");
        registry.register(new TestEntry(key, "Potato"));

        assertTrue(registry.contains(key));
        assertTrue(registry.unregister(key));
        assertFalse(registry.contains(key));
        assertEquals(0, registry.size());
        assertTrue(registry.get(key).isEmpty());
    }

    @Test
    @DisplayName("Unregister non-existent or null key returns false")
    void testUnregisterNonExistentKey() {
        assertFalse(registry.unregister(Key.croppy("non_existent")));
        assertFalse(registry.unregister(null));
    }

    @Test
    @DisplayName("UnregisterAll with predicate removes matching entries atomically")
    void testUnregisterAllWithPredicate() {
        registry.register(new TestEntry(Key.of("addon_a", "crop_1"), "Addon Crop 1"));
        registry.register(new TestEntry(Key.of("addon_a", "crop_2"), "Addon Crop 2"));
        registry.register(new TestEntry(Key.of("addon_b", "crop_3"), "Other Crop"));
        assertEquals(3, registry.size());

        int removed = registry.unregisterAll(entry -> "addon_a".equals(entry.getKey().namespace()));
        assertEquals(2, removed);
        assertEquals(1, registry.size());
        assertTrue(registry.contains(Key.of("addon_b", "crop_3")));
        assertFalse(registry.contains(Key.of("addon_a", "crop_1")));
        assertFalse(registry.contains(Key.of("addon_a", "crop_2")));
    }
}
