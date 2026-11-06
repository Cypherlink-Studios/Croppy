package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CropBreakListenerTest {

    private CroppyCore core;
    private DefaultItemRegistry itemRegistry;
    private ItemFactory itemFactory;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("test");
        core = new CroppyCore(logger);
        itemRegistry = new DefaultItemRegistry();
        itemFactory = new ItemFactory(null, itemRegistry);
    }

    @Test
    @DisplayName("Constructor preserves injected ItemFactory instance")
    void testConstructorInjection() throws Exception {
        CropBreakListener listener = new CropBreakListener(core, null, null, itemFactory);

        Field itemFactoryField = CropBreakListener.class.getDeclaredField("itemFactory");
        itemFactoryField.setAccessible(true);
        assertSame(itemFactory, itemFactoryField.get(listener));
    }

    @Test
    @DisplayName("Fallback constructor instantiates default ItemFactory")
    void testFallbackConstructor() throws Exception {
        CropBreakListener listener = new CropBreakListener(core);

        Field itemFactoryField = CropBreakListener.class.getDeclaredField("itemFactory");
        itemFactoryField.setAccessible(true);
        assertNotNull(itemFactoryField.get(listener));
    }

    @Test
    @DisplayName("CropBreakListener resolves custom item drops via ItemFactory")
    void testCustomItemDropResolution() {
        ItemDefinition tomatoDef = new ItemDefinition(
                Key.of("croppy", "tomato"),
                "ingredients",
                "minecraft:apple",
                2001,
                "<red>Tomato</red>",
                List.of(),
                Map.of()
        );
        itemRegistry.register(tomatoDef);

        AtomicReference<ItemDefinition> defCalled = new AtomicReference<>();
        AtomicInteger amountCalled = new AtomicInteger();

        ItemFactory testFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                defCalled.set(definition);
                amountCalled.set(amount);
                return null;
            }
        };

        Optional<ItemStack> result = testFactory.resolveItemStack("croppy:tomato", 3);
        assertEquals(tomatoDef, defCalled.get());
        assertEquals(3, amountCalled.get());
    }

    @Test
    @DisplayName("CropBreakListener resolves custom seed drops via ItemFactory")
    void testCustomSeedDropResolution() {
        ItemDefinition seedDef = new ItemDefinition(
                Key.of("croppy", "tomato_seeds"),
                "seeds",
                "minecraft:pumpkin_seeds",
                2002,
                "<red>Tomato Seeds</red>",
                List.of(),
                Map.of()
        );
        itemRegistry.register(seedDef);

        AtomicReference<ItemDefinition> defCalled = new AtomicReference<>();
        AtomicInteger amountCalled = new AtomicInteger();

        ItemFactory testFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                defCalled.set(definition);
                amountCalled.set(amount);
                return null;
            }
        };

        Optional<ItemStack> result = testFactory.resolveItemStack("croppy:tomato_seeds", 1);
        assertEquals(seedDef, defCalled.get());
        assertEquals(1, amountCalled.get());
    }
}
