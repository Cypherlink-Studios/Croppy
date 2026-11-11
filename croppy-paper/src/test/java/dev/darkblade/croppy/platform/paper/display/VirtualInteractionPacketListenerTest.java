package dev.darkblade.croppy.platform.paper.display;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import net.kyori.adventure.audience.Audience;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class VirtualInteractionPacketListenerTest {

    private CroppyCore core;
    private DefaultItemRegistry itemRegistry;
    private ItemFactory itemFactory;
    private Plugin mockPlugin;
    private VirtualBlockDisplayTracker displayTracker;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("test");
        core = new CroppyCore(logger);
        itemRegistry = new DefaultItemRegistry();
        itemFactory = new ItemFactory(null, itemRegistry);

        mockPlugin = (Plugin) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> null
        );

        Server mockServer = (Server) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> null
        );

        displayTracker = new VirtualBlockDisplayTracker(
                mockServer,
                new PacketEventsBlockRenderer(logger),
                itemFactory,
                logger
        );
    }

    @Test
    @DisplayName("Constructor injection preserves ItemFactory instance")
    void testConstructorInjection() throws Exception {
        VirtualInteractionPacketListener listener = new VirtualInteractionPacketListener(
                mockPlugin,
                core,
                displayTracker,
                Logger.getLogger("test"),
                null,
                null,
                itemFactory
        );

        Field itemFactoryField = VirtualInteractionPacketListener.class.getDeclaredField("itemFactory");
        itemFactoryField.setAccessible(true);
        assertSame(itemFactory, itemFactoryField.get(listener));
    }

    @Test
    @DisplayName("Fallback constructor instantiates default ItemFactory when null is passed")
    void testFallbackConstructorInstantiatesItemFactory() throws Exception {
        VirtualInteractionPacketListener listener = new VirtualInteractionPacketListener(
                mockPlugin,
                core,
                displayTracker,
                Logger.getLogger("test"),
                null,
                null
        );

        Field itemFactoryField = VirtualInteractionPacketListener.class.getDeclaredField("itemFactory");
        itemFactoryField.setAccessible(true);
        assertNotNull(itemFactoryField.get(listener));
    }

    @Test
    @DisplayName("ItemFactory resolves custom yield drops for crop harvest")
    void testCustomYieldDropResolution() {
        ItemDefinition grapeDef = new ItemDefinition(
                Key.of("croppy", "grapes"),
                "ingredients",
                "minecraft:sweet_berries",
                1001,
                "<purple>Sweet Grapes</purple>",
                List.of("<gray>Fresh grapes</gray>"),
                Map.of()
        );
        itemRegistry.register(grapeDef);

        AtomicReference<ItemDefinition> defCalled = new AtomicReference<>();
        AtomicInteger amountCalled = new AtomicInteger();

        ItemFactory mockFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                defCalled.set(definition);
                amountCalled.set(amount);
                return null;
            }
        };

        Optional<ItemStack> resolved = mockFactory.resolveItemStack("croppy:grapes", 4);
        assertEquals(grapeDef, defCalled.get());
        assertEquals(4, amountCalled.get());
    }

    @Test
    @DisplayName("ItemFactory resolves custom seed item for dismantling")
    void testCustomSeedDropResolution() {
        ItemDefinition seedDef = new ItemDefinition(
                Key.of("croppy", "grape_seeds"),
                "seeds",
                "minecraft:wheat_seeds",
                1002,
                "<green>Grape Seeds</green>",
                List.of(),
                Map.of()
        );
        itemRegistry.register(seedDef);

        AtomicReference<ItemDefinition> defCalled = new AtomicReference<>();
        AtomicInteger amountCalled = new AtomicInteger();

        ItemFactory mockFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                defCalled.set(definition);
                amountCalled.set(amount);
                return null;
            }
        };

        mockFactory.resolveItemStack("croppy:grape_seeds", 1);
        assertEquals(seedDef, defCalled.get());
        assertEquals(1, amountCalled.get());
    }

    @Test
    @DisplayName("Right-clicking virtual interaction entity with inspection tool triggers inspection and avoids harvest")
    void testRightClickWithInspectionToolInspectsCrop() throws Exception {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("golden_wheat"),
                pos,
                2, // mature stage
                1.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        core.getCropManager().registerCrop(crop);

        Species testSpecies = new Species() {
            private final Key key = Key.croppy("golden_wheat");
            private final List<StageDefinition> stages = List.of(
                    new StageDefinition("seed", 100.0, 0),
                    new StageDefinition("growing", 100.0, 3),
                    new StageDefinition("mature", 0.0, 7)
            );
            private final CropRequirements requirements = new CropRequirements(9, 1);
            @Override public Key getKey() { return key; }
            @Override public String getName() { return "Golden Wheat"; }
            @Override public CropDisplay getDisplay() { return new CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds"); }
            @Override public List<StageDefinition> getStages() { return stages; }
            @Override public CropRequirements getRequirements() { return requirements; }
            @Override public HarvestDefinition getHarvest() { return null; }
            @Override public boolean isMature(int stageIndex) { return stageIndex >= 2; }
        };
        core.getSpeciesRegistry().register(testSpecies);

        AtomicBoolean inspected = new AtomicBoolean(false);
        CropInspectorService mockInspector = new CropInspectorService(
                core.getCropManager(),
                core.getSpeciesRegistry(),
                p -> null
        ) {
            @Override
            public Optional<net.kyori.adventure.text.Component> inspect(BlockPosition position, Audience audience) {
                if (pos.equals(position)) {
                    inspected.set(true);
                    return Optional.of(net.kyori.adventure.text.Component.text("Crop Card"));
                }
                return Optional.empty();
            }
        };

        VirtualInteractionPacketListener listener = new VirtualInteractionPacketListener(
                mockPlugin,
                core,
                displayTracker,
                Logger.getLogger("test"),
                null,
                null,
                itemFactory,
                mockInspector
        );

        AtomicReference<net.kyori.adventure.text.Component> sentMessage = new AtomicReference<>();
        Player mockPlayer = createInspectionToolPlayer(sentMessage);

        invokeHandlePlayerInteraction(listener, mockPlayer, pos, WrapperPlayClientInteractEntity.InteractAction.INTERACT);

        assertTrue(inspected.get(), "Expected inspectorService.inspect() to be invoked");
        assertNotNull(sentMessage.get(), "Expected inspection card message to be sent to player");
        assertTrue(core.getCropManager().getCropAt(pos).isPresent(), "Crop must not be removed/harvested on inspect");
        assertEquals(2, core.getCropManager().getCropAt(pos).get().getStageIndex(), "Crop stage must remain unchanged");
    }

    @Test
    @DisplayName("Right-clicking virtual interaction entity with inspection tool succeeds even when harvest protection denies")
    void testRightClickWithInspectionToolBypassesHarvestProtection() throws Exception {
        BlockPosition pos = BlockPosition.of("world", 12, 64, 12);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("golden_wheat"),
                pos,
                2,
                1.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        core.getCropManager().registerCrop(crop);

        AtomicBoolean inspected = new AtomicBoolean(false);
        CropInspectorService mockInspector = new CropInspectorService(
                core.getCropManager(),
                core.getSpeciesRegistry(),
                p -> null
        ) {
            @Override
            public Optional<net.kyori.adventure.text.Component> inspect(BlockPosition position, Audience audience) {
                if (pos.equals(position)) {
                    inspected.set(true);
                    return Optional.of(net.kyori.adventure.text.Component.text("Protected Crop Card"));
                }
                return Optional.empty();
            }
        };

        dev.darkblade.croppy.api.protection.ProtectionPort denyingProtection = (playerId, position, action) -> false;

        VirtualInteractionPacketListener listener = new VirtualInteractionPacketListener(
                mockPlugin,
                core,
                displayTracker,
                Logger.getLogger("test"),
                null,
                denyingProtection,
                itemFactory,
                mockInspector
        );

        AtomicReference<net.kyori.adventure.text.Component> sentMessage = new AtomicReference<>();
        Player mockPlayer = createInspectionToolPlayer(sentMessage);

        invokeHandlePlayerInteraction(listener, mockPlayer, pos, WrapperPlayClientInteractEntity.InteractAction.INTERACT);

        assertTrue(inspected.get(), "Expected read-only inspection to bypass harvest protection");
        assertNotNull(sentMessage.get(), "Expected inspection card message even in protected zone");
        assertTrue(core.getCropManager().getCropAt(pos).isPresent());
    }

    private Player createInspectionToolPlayer(AtomicReference<net.kyori.adventure.text.Component> sentMessage) {
        Map<NamespacedKey, Object> pdcMap = new HashMap<>();
        pdcMap.put(new NamespacedKey("croppy", "item_id"), "croppy:inspection_tool");

        PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name)) return pdcMap.get((NamespacedKey) args[0]);
                    if ("has".equals(name)) return pdcMap.containsKey((NamespacedKey) args[0]);
                    return null;
                }
        );

        ItemMeta mockMeta = (ItemMeta) Proxy.newProxyInstance(
                ItemMeta.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> {
                    if ("getPersistentDataContainer".equals(method.getName())) return pdc;
                    return null;
                }
        );

        ItemStack mockItem = new ItemStack(Material.PAPER) {
            @Override
            public boolean hasItemMeta() {
                return true;
            }

            @Override
            public ItemMeta getItemMeta() {
                return mockMeta;
            }
        };

        PlayerInventory mockInv = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if ("getItemInMainHand".equals(method.getName())) return mockItem;
                    return null;
                }
        );

        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getInventory".equals(method.getName())) return mockInv;
                    if ("isSneaking".equals(method.getName())) return false;
                    if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                    if ("sendMessage".equals(method.getName())) {
                        sentMessage.set((net.kyori.adventure.text.Component) args[0]);
                        return null;
                    }
                    if ("isOnline".equals(method.getName())) return true;
                    if ("getGameMode".equals(method.getName())) return GameMode.SURVIVAL;
                    return null;
                }
        );
    }

    private void invokeHandlePlayerInteraction(
            VirtualInteractionPacketListener listener,
            Player player,
            BlockPosition pos,
            WrapperPlayClientInteractEntity.InteractAction action
    ) throws Exception {
        Method handleMethod = VirtualInteractionPacketListener.class.getDeclaredMethod(
                "handlePlayerInteraction",
                Player.class,
                BlockPosition.class,
                WrapperPlayClientInteractEntity.InteractAction.class
        );
        handleMethod.setAccessible(true);
        handleMethod.invoke(listener, player, pos, action);
    }
}
