package dev.darkblade.croppy.platform.paper.display;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
