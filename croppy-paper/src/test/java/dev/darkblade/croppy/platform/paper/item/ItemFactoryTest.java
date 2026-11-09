package dev.darkblade.croppy.platform.paper.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
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
                Key.of("croppy", "growth_fertilizer"),
                "fertilizers",
                "minecraft:bone_meal",
                101,
                "<green>Growth Fertilizer</green>",
                List.of("<gray>Growth booster</gray>"),
                Map.of("speed_multiplier", "1.25")
        );
        registry.register(def);

        assertEquals(1, registry.size());
        assertEquals("fertilizers", registry.get(Key.of("croppy", "growth_fertilizer")).get().category());

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
                Key.of("croppy", "growth_fertilizer"),
                "fertilizers",
                "minecraft:bone_meal",
                101,
                "<green>Growth Fertilizer</green>",
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

    @Test
    @DisplayName("createBukkitItem cleanly routes vanilla items without altering metadata")
    void testCreateBukkitItemVanillaUntouched() {
        AtomicReference<Material> vanillaMaterialCreated = new AtomicReference<>();
        AtomicInteger vanillaAmountCreated = new AtomicInteger();
        AtomicReference<ItemDefinition> customDefCreated = new AtomicReference<>();

        ItemFactory testFactory = new ItemFactory(null, null, registry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                customDefCreated.set(definition);
                return null;
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                vanillaMaterialCreated.set(material);
                vanillaAmountCreated.set(amount);
                return null;
            }
        };

        dev.darkblade.croppy.api.station.StoredItem stored = dev.darkblade.croppy.api.station.StoredItem.of(Key.of("minecraft", "apple"), 5);
        testFactory.createBukkitItem(stored);

        // Vanilla item must route straight to createRawVanillaItemStack with Material.APPLE and amount 5
        assertEquals(Material.APPLE, vanillaMaterialCreated.get());
        assertEquals(5, vanillaAmountCreated.get());
        assertNull(customDefCreated.get(), "Vanilla item must never be wrapped in a synthetic ItemDefinition");
    }

    @Test
    @DisplayName("createBukkitItem resolves custom registered item with definition")
    void testCreateBukkitItemCustomRegistered() {
        ItemDefinition customDef = new ItemDefinition(
                Key.of("croppy", "grape_wine"),
                "artisan",
                "minecraft:potion",
                501,
                "<purple>Grape Wine</purple>",
                List.of("<gray>Fine vintage</gray>"),
                Map.of("quality", "gold")
        );
        registry.register(customDef);

        AtomicReference<ItemDefinition> customDefCreated = new AtomicReference<>();
        AtomicInteger customAmountCreated = new AtomicInteger();
        AtomicReference<Material> vanillaMaterialCreated = new AtomicReference<>();

        ItemFactory testFactory = new ItemFactory(null, null, registry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                customDefCreated.set(definition);
                customAmountCreated.set(amount);
                return null;
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                vanillaMaterialCreated.set(material);
                return null;
            }
        };

        dev.darkblade.croppy.api.station.StoredItem stored = dev.darkblade.croppy.api.station.StoredItem.of(Key.of("croppy", "grape_wine"), 2);
        testFactory.createBukkitItem(stored);

        assertEquals(customDef, customDefCreated.get());
        assertEquals(2, customAmountCreated.get());
        assertNull(vanillaMaterialCreated.get());
    }

    @Test
    @DisplayName("resolveItemStack correctly resolves custom registered items")
    void testResolveItemStackCustom() {
        ItemDefinition grapesDef = new ItemDefinition(
                Key.of("croppy", "grapes"),
                "ingredients",
                "minecraft:sweet_berries",
                1001,
                "<purple>Sweet Grapes</purple>",
                List.of("<gray>Fresh grapes</gray>"),
                Map.of()
        );
        registry.register(grapesDef);

        AtomicReference<ItemDefinition> customDefCalled = new AtomicReference<>();
        AtomicInteger customAmountCalled = new AtomicInteger();

        ItemFactory testFactory = new ItemFactory(null, null, registry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                customDefCalled.set(definition);
                customAmountCalled.set(amount);
                return null;
            }
        };

        testFactory.resolveItemStack("croppy:grapes", 3);
        assertEquals(grapesDef, customDefCalled.get());
        assertEquals(3, customAmountCalled.get());
    }

    @Test
    @DisplayName("resolveItemStack cleanly resolves vanilla materials")
    void testResolveItemStackVanilla() {
        AtomicReference<Material> vanillaMaterialCalled = new AtomicReference<>();
        AtomicInteger vanillaAmountCalled = new AtomicInteger();

        ItemFactory testFactory = new ItemFactory(null, null, registry) {
            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                vanillaMaterialCalled.set(material);
                vanillaAmountCalled.set(amount);
                return null;
            }
        };

        testFactory.resolveItemStack("minecraft:oak_fence", 2);
        assertEquals(Material.OAK_FENCE, vanillaMaterialCalled.get());
        assertEquals(2, vanillaAmountCalled.get());

        testFactory.resolveItemStack("wheat", 4);
        assertEquals(Material.WHEAT, vanillaMaterialCalled.get());
        assertEquals(4, vanillaAmountCalled.get());
    }

    @Test
    @DisplayName("resolveItemStack returns empty for invalid, unknown, or non-positive arguments")
    void testResolveItemStackEdgeCases() {
        ItemFactory testFactory = new ItemFactory(null, null, registry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                return null;
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                return null;
            }
        };

        assertTrue(testFactory.resolveItemStack(null, 1).isEmpty());
        assertTrue(testFactory.resolveItemStack("", 1).isEmpty());
        assertTrue(testFactory.resolveItemStack("   ", 1).isEmpty());
        assertTrue(testFactory.resolveItemStack("croppy:grapes", 0).isEmpty());
        assertTrue(testFactory.resolveItemStack("croppy:grapes", -2).isEmpty());
        assertTrue(testFactory.resolveItemStack("croppy:nonexistent_item_xyz", 1).isEmpty());
        assertTrue(testFactory.resolveItemStack("not_a_valid_material_12345", 1).isEmpty());
        assertTrue(testFactory.resolveItemStack("minecraft:air", 1).isEmpty());
        assertTrue(testFactory.resolveItemStack("air", 1).isEmpty());
    }

    @Test
    @DisplayName("resolveBase64Texture handles raw base64, Mojang URLs, and hash formats")
    void testResolveBase64TextureFormats() {
        String base64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTIzIn19fQ==";
        assertEquals(base64, itemFactory.resolveBase64Texture(base64));

        String url = "http://textures.minecraft.net/texture/abcdef123456";
        String encodedUrl = itemFactory.resolveBase64Texture(url);
        assertNotNull(encodedUrl);
        String decodedUrl = new String(Base64.getDecoder().decode(encodedUrl), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(decodedUrl.contains("http://textures.minecraft.net/texture/abcdef123456"));

        String hash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        String encodedHash = itemFactory.resolveBase64Texture(hash);
        assertNotNull(encodedHash);
        String decodedHash = new String(Base64.getDecoder().decode(encodedHash), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(decodedHash.contains("http://textures.minecraft.net/texture/" + hash));
    }

    @Test
    @DisplayName("createProfileFromTexture generates deterministic UUID and textures property via server")
    void testCreateProfileFromTexture() {
        AtomicReference<UUID> profileUuid = new AtomicReference<>();
        List<ProfileProperty> capturedProps = new ArrayList<>();

        Server mockServer = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if ("createProfile".equals(method.getName())) {
                        UUID uuid = (UUID) args[0];
                        profileUuid.set(uuid);
                        return Proxy.newProxyInstance(
                                PlayerProfile.class.getClassLoader(),
                                new Class<?>[]{PlayerProfile.class},
                                (pProxy, pMethod, pArgs) -> {
                                    if ("getId".equals(pMethod.getName())) return uuid;
                                    if ("setProperty".equals(pMethod.getName())) {
                                        capturedProps.add((ProfileProperty) pArgs[0]);
                                        return null;
                                    }
                                    if ("getProperties".equals(pMethod.getName())) return Set.copyOf(capturedProps);
                                    return null;
                                }
                        );
                    }
                    return null;
                }
        );

        ItemFactory testFactory = new ItemFactory(null, mockServer, registry);
        String base64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTk5In19fQ==";
        PlayerProfile profile = testFactory.createProfileFromTexture(base64);

        assertNotNull(profile);
        UUID expectedUuid = UUID.nameUUIDFromBytes(base64.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(expectedUuid, profileUuid.get());

        boolean hasTextureProp = capturedProps.stream()
                .anyMatch(p -> "textures".equals(p.getName()) && base64.equals(p.getValue()));
        assertTrue(hasTextureProp, "PlayerProfile must contain 'textures' property with base64 payload");
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
