package dev.darkblade.croppy.platform.paper.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    @DisplayName("Verify NamespacedKey creation and ItemDefinition integration")
    void testItemDefinitionIntegration() {
        DefaultItemRegistry registry = new DefaultItemRegistry();
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "speed_gro"),
                "fertilizers",
                "minecraft:bone_meal",
                101,
                "<green>Speed-Gro</green>",
                List.of("<gray>Growth booster</gray>"),
                Map.of("speed_multiplier", "1.25")
        );
        registry.register(def);

        assertEquals(1, registry.size());
        assertEquals("fertilizers", registry.get(Key.of("croppy", "speed_gro")).get().category());

        NamespacedKey itemIdKey = new NamespacedKey("croppy", "item_id");
        NamespacedKey categoryKey = new NamespacedKey("croppy", "category");
        assertEquals("croppy:item_id", itemIdKey.toString());
        assertEquals("croppy:category", categoryKey.toString());
    }
}
