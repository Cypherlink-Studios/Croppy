package dev.darkblade.croppy.platform.paper.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    private DefaultItemRegistry registry;
    private ItemFactory itemFactory;

    @BeforeEach
    void setUp() {
        registry = new DefaultItemRegistry();
        itemFactory = new ItemFactory(null, registry);
    }

    @Test
    @DisplayName("Verify NamespacedKey creation and ItemDefinition integration")
    void testItemDefinitionIntegration() {
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
        assertEquals(itemIdKey, itemFactory.getItemIdKey());
        assertEquals(categoryKey, itemFactory.getCategoryKey());
    }

    @Test
    @DisplayName("applyToMeta sets both customModelData and native itemModel when present")
    void testApplyToMetaWithBothCmdAndItemModel() {
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "copper_can"),
                "watering_cans",
                "minecraft:copper_ingot",
                2002,
                Key.of("croppy", "copper_can"),
                "<red>Copper Can</red>",
                List.of("<gray>Watering can</gray>"),
                Map.of("capacity", "40")
        );

        AtomicInteger cmdSet = new AtomicInteger(-1);
        AtomicReference<NamespacedKey> itemModelSet = new AtomicReference<>();
        AtomicReference<Component> nameSet = new AtomicReference<>();
        AtomicReference<List<Component>> loreSet = new AtomicReference<>();
        Map<NamespacedKey, Object> pdcMap = new HashMap<>();

        ItemMeta metaProxy = createMockItemMeta(cmdSet, itemModelSet, nameSet, loreSet, pdcMap);

        itemFactory.applyToMeta(metaProxy, def);

        assertEquals(2002, cmdSet.get(), "Custom model data should be applied");
        assertNotNull(itemModelSet.get(), "Item model should be applied");
        assertEquals(new NamespacedKey("croppy", "copper_can"), itemModelSet.get());
        assertNotNull(nameSet.get(), "Display name should be set");
        assertNotNull(loreSet.get(), "Lore should be set");
        assertEquals(1, loreSet.get().size());
        assertEquals("croppy:copper_can", pdcMap.get(new NamespacedKey("croppy", "item_id")));
        assertEquals("watering_cans", pdcMap.get(new NamespacedKey("croppy", "category")));
        assertEquals("40", pdcMap.get(new NamespacedKey("croppy", "capacity")));
    }

    @Test
    @DisplayName("applyToMeta does not set itemModel when null on definition")
    void testApplyToMetaWithoutItemModel() {
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "speed_gro"),
                "fertilizers",
                "minecraft:bone_meal",
                101,
                "<green>Speed-Gro</green>",
                List.of("<gray>Growth booster</gray>"),
                Map.of()
        );

        AtomicInteger cmdSet = new AtomicInteger(-1);
        AtomicReference<NamespacedKey> itemModelSet = new AtomicReference<>();
        Map<NamespacedKey, Object> pdcMap = new HashMap<>();

        ItemMeta metaProxy = createMockItemMeta(cmdSet, itemModelSet, new AtomicReference<>(), new AtomicReference<>(), pdcMap);

        itemFactory.applyToMeta(metaProxy, def);

        assertEquals(101, cmdSet.get());
        assertNull(itemModelSet.get(), "Item model should NOT be set when null");
    }

    @Test
    @DisplayName("applyToMeta sets only itemModel when customModelData is 0")
    void testApplyToMetaWithOnlyItemModel() {
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "modern_crop"),
                "crops",
                "minecraft:wheat",
                0,
                Key.of("croppy", "modern_crop"),
                "Modern Crop",
                List.of(),
                Map.of()
        );

        AtomicInteger cmdSet = new AtomicInteger(-1);
        AtomicReference<NamespacedKey> itemModelSet = new AtomicReference<>();
        Map<NamespacedKey, Object> pdcMap = new HashMap<>();

        ItemMeta metaProxy = createMockItemMeta(cmdSet, itemModelSet, new AtomicReference<>(), new AtomicReference<>(), pdcMap);

        itemFactory.applyToMeta(metaProxy, def);

        assertEquals(-1, cmdSet.get(), "Custom model data should NOT be set when 0");
        assertNotNull(itemModelSet.get(), "Item model should be set");
        assertEquals(new NamespacedKey("croppy", "modern_crop"), itemModelSet.get());
    }

    private ItemMeta createMockItemMeta(
            AtomicInteger cmdRef,
            AtomicReference<NamespacedKey> modelRef,
            AtomicReference<Component> nameRef,
            AtomicReference<List<Component>> loreRef,
            Map<NamespacedKey, Object> pdcMap
    ) {
        PersistentDataContainer pdcProxy = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("set".equals(name)) {
                        pdcMap.put((NamespacedKey) args[0], args[2]);
                        return null;
                    } else if ("get".equals(name)) {
                        return pdcMap.get((NamespacedKey) args[0]);
                    } else if ("has".equals(name)) {
                        return pdcMap.containsKey((NamespacedKey) args[0]);
                    }
                    return null;
                }
        );

        return (ItemMeta) Proxy.newProxyInstance(
                ItemMeta.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "setCustomModelData" -> {
                            cmdRef.set((Integer) args[0]);
                            return null;
                        }
                        case "setItemModel" -> {
                            modelRef.set((NamespacedKey) args[0]);
                            return null;
                        }
                        case "hasItemModel" -> {
                            return modelRef.get() != null;
                        }
                        case "getItemModel" -> {
                            return modelRef.get();
                        }
                        case "getPersistentDataContainer" -> {
                            return pdcProxy;
                        }
                        case "displayName" -> {
                            if (args != null && args.length > 0 && args[0] instanceof Component c) {
                                nameRef.set(c);
                            }
                            return null;
                        }
                        case "lore" -> {
                            if (args != null && args.length > 0 && args[0] instanceof List<?> list) {
                                @SuppressWarnings("unchecked")
                                List<Component> clist = (List<Component>) list;
                                loreRef.set(clist);
                            }
                            return null;
                        }
                        default -> {
                            return null;
                        }
                    }
                }
        );
    }
}
