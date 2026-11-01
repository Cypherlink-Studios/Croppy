package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeKnowledgeBinaryCodecTest {

    @Test
    @DisplayName("Round-trip serialization and deserialization of player recipe knowledge")
    void testSerializationRoundTrip() {
        UUID playerId = UUID.randomUUID();
        Key recipe1 = Key.of("croppy", "tomato_soup");
        Key recipe2 = Key.of("croppy", "artisan_bread");

        PlayerRecipeKnowledge k1 = new PlayerRecipeKnowledge(
                playerId,
                recipe1,
                KnowledgeState.DISCOVERED,
                Instant.now(),
                Key.of("croppy", "experimental"),
                Map.of("note", "secret spices")
        );

        PlayerRecipeKnowledge k2 = new PlayerRecipeKnowledge(
                playerId,
                recipe2,
                KnowledgeState.HINTED,
                Instant.now(),
                Key.of("croppy", "world"),
                Map.of("clue", "found in dungeon chest")
        );

        byte[] serialized = RecipeKnowledgeBinaryCodec.serialize(List.of(k1, k2));
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        Map<Key, PlayerRecipeKnowledge> deserialized = RecipeKnowledgeBinaryCodec.deserialize(serialized, playerId);
        assertEquals(2, deserialized.size());

        PlayerRecipeKnowledge res1 = deserialized.get(recipe1);
        assertNotNull(res1);
        assertEquals(playerId, res1.playerId());
        assertEquals(KnowledgeState.DISCOVERED, res1.state());
        assertEquals(Key.of("croppy", "experimental"), res1.discoveryMethodKey());
        assertEquals("secret spices", res1.metadata().get("note"));

        PlayerRecipeKnowledge res2 = deserialized.get(recipe2);
        assertNotNull(res2);
        assertEquals(KnowledgeState.HINTED, res2.state());
        assertEquals(Key.of("croppy", "world"), res2.discoveryMethodKey());
        assertEquals("found in dungeon chest", res2.metadata().get("clue"));
    }

    @Test
    @DisplayName("Handles empty and null byte arrays safely")
    void testEmptyAndNull() {
        UUID playerId = UUID.randomUUID();
        byte[] empty = RecipeKnowledgeBinaryCodec.serialize(List.of());
        assertEquals(0, empty.length);

        assertTrue(RecipeKnowledgeBinaryCodec.deserialize(new byte[0], playerId).isEmpty());
        assertTrue(RecipeKnowledgeBinaryCodec.deserialize(null, playerId).isEmpty());
    }
}
