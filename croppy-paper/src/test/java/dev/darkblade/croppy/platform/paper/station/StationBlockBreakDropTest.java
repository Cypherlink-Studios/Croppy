package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.DefaultStationRegistry;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.listener.StationBlockListener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StationBlockBreakDropTest {

    @Test
    @DisplayName("Verify createStationItem resolves custom item from registry")
    void testCreateStationItemFromRegistry() {
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        Key potKey = Key.of("croppy", "cooking_pot");
        ItemDefinition itemDef = new ItemDefinition(
                potKey,
                "stations",
                "minecraft:player_head",
                0,
                "<gradient:#ffaa00:#ff5555>Cooking Pot</gradient>",
                List.of("<gray>Cookware</gray>"),
                Map.of()
        );
        itemRegistry.register(itemDef);

        AtomicInteger createCalls = new AtomicInteger(0);
        ItemFactory itemFactory = new ItemFactory(null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                createCalls.incrementAndGet();
                assertEquals(potKey, definition.key());
                assertEquals(1, amount);
                return null; // Return stub
            }
        };

        StationDefinition stationDef = new StationDefinition(
                potKey,
                "Cooking Pot",
                "minecraft:player_head",
                StationInteractionType.HYBRID,
                6,
                2,
                true,
                List.of(),
                Map.of()
        );
        DefaultStationInstance station = new DefaultStationInstance(potKey, BlockPosition.of("world", 0, 64, 0), stationDef);

        dev.darkblade.croppy.api.storage.StationStorage dummyStorage = new dev.darkblade.croppy.api.storage.StationStorage() {
            @Override public java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> loadChunk(String w, int x, int z) { return List.of(); }
            @Override public void saveChunk(String w, int x, int z, java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> s) {}
            @Override public void deleteChunk(String w, int x, int z) {}
            @Override public void saveStation(dev.darkblade.croppy.api.station.StationInstance s) {}
            @Override public void deleteStation(BlockPosition p) {}
        };

        StationBlockListener listener = new StationBlockListener(
                new DefaultStationManager(),
                new DefaultStationRegistry(),
                dummyStorage,
                itemFactory
        );

        listener.createStationItem(station);
        assertEquals(1, createCalls.get(), "createItemStack should be called for station custom item");
    }

    @Test
    @DisplayName("Verify createBukkitItem resolves stored items from registry")
    void testCreateBukkitItemFromRegistry() {
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        Key carrotKey = Key.of("croppy", "carrot");
        ItemDefinition itemDef = new ItemDefinition(
                carrotKey,
                "crops",
                "minecraft:carrot",
                0,
                "<gold>Carrot</gold>",
                List.of(),
                Map.of()
        );
        itemRegistry.register(itemDef);

        List<Key> requestedKeys = new ArrayList<>();
        ItemFactory itemFactory = new ItemFactory(null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                requestedKeys.add(definition.key());
                return null;
            }
        };

        dev.darkblade.croppy.api.storage.StationStorage dummyStorage = new dev.darkblade.croppy.api.storage.StationStorage() {
            @Override public java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> loadChunk(String w, int x, int z) { return List.of(); }
            @Override public void saveChunk(String w, int x, int z, java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> s) {}
            @Override public void deleteChunk(String w, int x, int z) {}
            @Override public void saveStation(dev.darkblade.croppy.api.station.StationInstance s) {}
            @Override public void deleteStation(BlockPosition p) {}
        };

        StationBlockListener listener = new StationBlockListener(
                new DefaultStationManager(),
                new DefaultStationRegistry(),
                dummyStorage,
                itemFactory
        );

        listener.createBukkitItem(StoredItem.of(carrotKey, 3));
        assertEquals(1, requestedKeys.size());
        assertEquals(carrotKey, requestedKeys.get(0));
    }

    @Test
    @DisplayName("Verify station destruction removes station from manager")
    void testStationDestructionRemoval() {
        DefaultStationManager stationManager = new DefaultStationManager();
        BlockPosition pos = BlockPosition.of("world", 10, 65, 10);
        Key skilletKey = Key.of("croppy", "skillet");

        StationDefinition def = new StationDefinition(
                skilletKey,
                "Skillet",
                "minecraft:heavy_weighted_pressure_plate",
                StationInteractionType.PHYSICAL,
                4,
                2,
                true,
                List.of(),
                Map.of()
        );
        DefaultStationInstance station = new DefaultStationInstance(skilletKey, pos, def);
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addInput(StoredItem.of(Key.of("minecraft", "beef"), 2));
        station.setComponent(StorageComponent.class, storage);

        stationManager.addStation(station);
        assertTrue(stationManager.getStation(pos).isPresent());

        // Remove from manager as onBlockBreak does
        var removed = stationManager.removeStation(pos);
        assertTrue(removed.isPresent());
        assertTrue(stationManager.getStation(pos).isEmpty());
        assertEquals(1, removed.get().getComponent(StorageComponent.class).get().getInputs().size());
    }

    @Test
    @DisplayName("Verify placement facing calculation faces player with CARDINAL_4 snapping")
    void testStationPlacementFacing() {
        // Player facing South (yaw = 0.0)
        float playerYaw = 0.0f;
        // Station front faces player (playerYaw + 180.0) -> 180.0
        float effectiveYaw = dev.darkblade.croppy.core.species.RotationCalculator.calculate(
                playerYaw + 180.0f,
                dev.darkblade.croppy.api.species.RotationMode.CARDINAL_4
        );
        assertEquals(180.0f, effectiveYaw, 0.001f);

        // Player facing West (yaw = 90.0)
        playerYaw = 90.0f;
        effectiveYaw = dev.darkblade.croppy.core.species.RotationCalculator.calculate(
                playerYaw + 180.0f,
                dev.darkblade.croppy.api.species.RotationMode.CARDINAL_4
        );
        assertEquals(270.0f, effectiveYaw, 0.001f);
    }
}
