package dev.darkblade.croppy.platform.paper.watering;

import dev.darkblade.croppy.api.world.BlockPosition;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WateringCanServiceTest {

    private Plugin createMockPlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "croppy";
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Verify WateringCanTier parsing and fallbacks")
    void testTierParsing() {
        assertEquals(WateringCanTier.RUSTY, WateringCanTier.parse("rusty"));
        assertEquals(WateringCanTier.COPPER, WateringCanTier.parse("copper"));
        assertEquals(WateringCanTier.SILVER, WateringCanTier.parse("silver"));
        assertEquals(WateringCanTier.GOLD, WateringCanTier.parse("gold"));
        assertEquals(WateringCanTier.IRIDIUM, WateringCanTier.parse("iridium"));
        assertEquals(WateringCanTier.COPPER, WateringCanTier.parse(null));
        assertEquals(WateringCanTier.COPPER, WateringCanTier.parse("unknown"));
    }

    @Test
    @DisplayName("Verify AoE calculations for 1x1, 1x3, and 3x3 tiers")
    void testAoeCalculation() {
        try {
            WateringCanService service = new WateringCanService(createMockPlugin());
            BlockPosition center = BlockPosition.of("world", 10, 64, 10);

            // 1x1
            List<BlockPosition> aoe1 = service.calculateAoe(center, BlockFace.NORTH, WateringCanTier.RUSTY);
            assertEquals(1, aoe1.size());
            assertEquals(center, aoe1.get(0));

            // 1x3 North/South
            List<BlockPosition> aoe3 = service.calculateAoe(center, BlockFace.NORTH, WateringCanTier.COPPER);
            assertEquals(3, aoe3.size());

            // 3x3 Silver
            List<BlockPosition> aoe9 = service.calculateAoe(center, BlockFace.NORTH, WateringCanTier.SILVER);
            assertEquals(9, aoe9.size());

            // 5x5 Iridium
            List<BlockPosition> aoe25 = service.calculateAoe(center, BlockFace.NORTH, WateringCanTier.IRIDIUM);
            assertEquals(25, aoe25.size());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
