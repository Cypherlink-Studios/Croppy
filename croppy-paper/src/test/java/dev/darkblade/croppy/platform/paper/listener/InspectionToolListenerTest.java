package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.service.CropTargetResolver;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InspectionToolListenerTest {

    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CropManager cropManager;
    private DefaultSpeciesRegistry speciesRegistry;
    private CropInspectorService inspectorService;
    private CroppyPlugin mockPlugin;
    private AtomicReference<MessageKey> sentMessageKey;
    private AtomicReference<Component> sentPlayerMessage;
    private World mockWorld;
    private Player mockPlayer;

    @BeforeEach
    void setUp() throws Exception {
        cropManager = new CropManager();
        speciesRegistry = new DefaultSpeciesRegistry();
        sentMessageKey = new AtomicReference<>();
        sentPlayerMessage = new AtomicReference<>();

        Species testSpecies = new Species() {
            private final Key key = Key.croppy("wheat");
            private final List<StageDefinition> stages = List.of(new StageDefinition("stage_1", 100.0, 0));
            private final CropRequirements requirements = new CropRequirements(8, 1);

            @Override public Key getKey() { return key; }
            @Override public String getName() { return "Wheat"; }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() {
                return new dev.darkblade.croppy.api.species.CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds");
            }
            @Override public List<StageDefinition> getStages() { return stages; }
            @Override public CropRequirements getRequirements() { return requirements; }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return null; }
        };
        speciesRegistry.register(testSpecies);

        inspectorService = new CropInspectorService(
                cropManager,
                speciesRegistry,
                pos -> CropEnvironment.of(12, 5)
        );

        mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "world";
                    return null;
                }
        );

        mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("getLocation".equals(method.getName())) return new Location(mockWorld, 10, 65, 10);
                    if ("getEyeLocation".equals(method.getName())) return new Location(mockWorld, 10, 66.6, 10);
                    if ("isOnline".equals(method.getName())) return true;
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof Component comp) {
                        sentPlayerMessage.set(comp);
                        return null;
                    }
                    return null;
                }
        );

        dev.darkblade.croppy.core.i18n.DefaultMessageCatalog catalog = new dev.darkblade.croppy.core.i18n.DefaultMessageCatalog();
        PaperMessageService mockMessageService = new PaperMessageService(catalog, (dev.darkblade.croppy.api.logging.CroppyLogger) null) {
            @Override
            public void sendPrefixedMessage(Audience audience, MessageKey key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
                sentMessageKey.set(key);
            }
        };

        mockPlugin = (CroppyPlugin) UNSAFE.allocateInstance(CroppyPlugin.class);
        Field msgField = CroppyPlugin.class.getDeclaredField("messageService");
        msgField.setAccessible(true);
        msgField.set(mockPlugin, mockMessageService);
    }

    private ItemStack createInspectionTool() {
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

        return new ItemStack(Material.STICK) {
            @Override
            public boolean hasItemMeta() {
                return true;
            }

            @Override
            public ItemMeta getItemMeta() {
                return mockMeta;
            }
        };
    }

    private Block createMockBlock(int x, int y, int z) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> mockWorld;
                    case "getX" -> x;
                    case "getY" -> y;
                    case "getZ" -> z;
                    case "getLocation" -> new Location(mockWorld, x, y, z);
                    default -> null;
                }
        );
    }

    @Test
    @DisplayName("Right click block with inspection tool cancels event and displays crop status")
    void testRightClickBlockWithInspectionToolCancelsAndInspects() {
        BlockPosition cropPos = BlockPosition.of("world", 10, 65, 10);
        CropInstance crop = new DefaultCropInstance(Key.croppy("wheat"), cropPos);
        cropManager.registerCrop(crop, 1);

        InspectionToolListener listener = new InspectionToolListener(mockPlugin, inspectorService);

        Block block = createMockBlock(10, 65, 10);
        ItemStack tool = createInspectionTool();

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, tool, block, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Interaction must be cancelled immediately at lowest priority");
        assertNotNull(sentPlayerMessage.get(), "Crop status card must be delivered to player");
    }

    @Test
    @DisplayName("Right click supporting farmland resolves crop at Y+1 and cancels event")
    void testRightClickSupportingFarmlandResolvesCropAbove() {
        BlockPosition cropPos = BlockPosition.of("world", 20, 65, 20);
        CropInstance crop = new DefaultCropInstance(Key.croppy("wheat"), cropPos);
        cropManager.registerCrop(crop, 1);

        InspectionToolListener listener = new InspectionToolListener(mockPlugin, inspectorService);

        // Click farmland at Y=64
        Block farmlandBlock = createMockBlock(20, 64, 20);
        ItemStack tool = createInspectionTool();

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, tool, farmlandBlock, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Interaction on supporting farmland must be cancelled");
        assertNotNull(sentPlayerMessage.get(), "Crop status card must be delivered for crop at Y+1");
    }

    @Test
    @DisplayName("Right click air with inspection tool queries CropTargetResolver and inspects line-of-sight crop")
    void testRightClickAirWithInspectionToolQueriesTargetResolver() {
        BlockPosition cropPos = BlockPosition.of("world", 30, 65, 30);
        CropInstance crop = new DefaultCropInstance(Key.croppy("wheat"), cropPos);
        cropManager.registerCrop(crop, 1);

        CropTargetResolver mockResolver = new CropTargetResolver(cropManager) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player, double maxDistance) {
                return Optional.of(new TargetResult(cropPos, crop, TargetResult.TargetType.PHYSICAL_BLOCK));
            }
        };

        InspectionToolListener listener = new InspectionToolListener(mockPlugin, inspectorService, mockResolver);
        ItemStack tool = createInspectionTool();

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_AIR, tool, null, null, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Right-click air must be cancelled");
        assertNotNull(sentPlayerMessage.get(), "Crop status card must be delivered from target resolver result");
    }

    @Test
    @DisplayName("Right click air with no crop in line of sight cancels event and sends no-crop-found")
    void testRightClickAirNoCropInLineOfSightSendsNoCropFound() {
        CropTargetResolver mockResolver = new CropTargetResolver(cropManager) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player, double maxDistance) {
                return Optional.empty();
            }
        };

        InspectionToolListener listener = new InspectionToolListener(mockPlugin, inspectorService, mockResolver);
        ItemStack tool = createInspectionTool();

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_AIR, tool, null, null, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Right-click air must be cancelled");
        assertNull(sentPlayerMessage.get(), "No status card sent");
        assertEquals(MessageKey.of("command.inspect.no-crop-found"), sentMessageKey.get(), "Must send no-crop-found message");
    }

    @Test
    @DisplayName("Normal stick or non-inspection tool does not cancel event")
    void testNonInspectionToolDoesNotCancelEvent() {
        InspectionToolListener listener = new InspectionToolListener(mockPlugin, inspectorService);

        ItemStack normalStick = new ItemStack(Material.STICK) {
            @Override
            public boolean hasItemMeta() {
                return false;
            }
        };
        Block block = createMockBlock(10, 65, 10);

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, normalStick, block, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertFalse(event.isCancelled(), "Event must not be cancelled for normal items");
        assertNull(sentPlayerMessage.get(), "No message sent");
        assertNull(sentMessageKey.get(), "No error key sent");
    }
}
