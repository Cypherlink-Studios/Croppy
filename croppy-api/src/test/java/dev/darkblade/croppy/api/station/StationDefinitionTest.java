package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.RotationMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StationDefinitionTest {

    @Test
    @DisplayName("Canonical constructor retains rotationMode and defaults correctly")
    void testCanonicalConstructorWithRotationMode() {
        Key stationKey = Key.of("croppy", "skillet");

        StationDefinition def = new StationDefinition(
                stationKey,
                "Skillet",
                "minecraft:iron_block",
                StationInteractionType.PHYSICAL,
                2,
                1,
                null,
                true,
                List.of(),
                Map.of(),
                null,
                RotationMode.FREE_YAW
        );

        assertEquals(stationKey, def.key());
        assertEquals(stationKey, def.getKey());
        assertEquals("Skillet", def.displayName());
        assertEquals(RotationMode.FREE_YAW, def.rotationMode());
        assertEquals(StationDefinition.DEFAULT_MENU_KEY, def.menuKey());
    }

    @Test
    @DisplayName("Backward-compatible constructor defaults rotationMode to CARDINAL_4")
    void testBackwardCompatibleConstructor() {
        Key stationKey = Key.of("croppy", "keg");

        StationDefinition def = new StationDefinition(
                stationKey,
                "Keg",
                "minecraft:barrel",
                StationInteractionType.GUI,
                1,
                1,
                true,
                List.of(),
                Map.of()
        );

        assertEquals(stationKey, def.key());
        assertEquals(RotationMode.CARDINAL_4, def.rotationMode());
    }

    @Test
    @DisplayName("Null validation and immutable defensive copies")
    void testValidationAndImmutability() {
        Key stationKey = Key.of("croppy", "test_station");

        assertThrows(NullPointerException.class, () -> new StationDefinition(
                null, "Name", "block", StationInteractionType.PHYSICAL, 1, 1, null, false, null, null, null, null
        ));

        assertThrows(NullPointerException.class, () -> new StationDefinition(
                stationKey, null, "block", StationInteractionType.PHYSICAL, 1, 1, null, false, null, null, null, null
        ));

        assertThrows(NullPointerException.class, () -> new StationDefinition(
                stationKey, "Name", null, StationInteractionType.PHYSICAL, 1, 1, null, false, null, null, null, null
        ));

        StationDefinition def = new StationDefinition(
                stationKey,
                "Test",
                "minecraft:stone",
                StationInteractionType.PHYSICAL,
                1,
                1,
                null,
                false,
                null,
                null,
                null,
                null
        );

        assertEquals(RotationMode.CARDINAL_4, def.rotationMode());
        assertNotNull(def.allowedRecipes());
        assertTrue(def.allowedRecipes().isEmpty());
        assertNotNull(def.properties());
        assertTrue(def.properties().isEmpty());
    }
}
