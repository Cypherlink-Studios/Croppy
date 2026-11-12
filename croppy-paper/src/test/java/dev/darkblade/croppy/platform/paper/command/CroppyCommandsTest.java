package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.logging.PaperCroppyLogger;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.service.CropTargetResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CroppyCommandsTest {

    private DefaultItemRegistry itemRegistry;
    private DefaultFertilizerRegistry fertilizerRegistry;
    private CroppyCommands commands;

    @BeforeEach
    void setUp() {
        itemRegistry = new DefaultItemRegistry();
        fertilizerRegistry = new DefaultFertilizerRegistry();
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        commands = new CroppyCommands(
                null,
                inspector,
                new PluginConfig(),
                null,
                null,
                fertilizerRegistry,
                null,
                itemRegistry
        );
    }

    @Test
    @DisplayName("Verify item categories suggestion provider")
    void testSuggestCategories() {
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "wheat_seeds"), "seeds", "WHEAT_SEEDS", 0, "Seeds", List.of(), Map.of()));
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "copper_can"), "watering_cans", "COPPER_INGOT", 0, "Can", List.of(), Map.of()));

        List<String> categories = commands.suggestItemCategories(null, CommandInput.empty());
        assertEquals(2, categories.size());
        assertTrue(categories.contains("seeds"));
        assertTrue(categories.contains("watering_cans"));
    }

    @Test
    @DisplayName("Verify category items suggestion provider")
    void testSuggestCategoryItems() {
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "wooden_can"), "watering_cans", "WOODEN_HOE", 0, "Wooden Can", List.of(), Map.of()));
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "wheat_seeds"), "seeds", "WHEAT_SEEDS", 0, "Seeds", List.of(), Map.of()));

        List<String> allItems = commands.suggestCategoryItems(null, CommandInput.empty());
        assertEquals(2, allItems.size());
        assertTrue(allItems.contains("croppy:wooden_can"));
        assertTrue(allItems.contains("croppy:wheat_seeds"));
    }

    @Test
    @DisplayName("Verify fertilizer items suggestion provider")
    void testSuggestFertilizerItems() {
        fertilizerRegistry.register(new Fertilizer(
                Key.of("croppy", "basic_fertilizer"),
                "Basic Fertilizer",
                List.of(),
                FertilizerType.SPEED,
                1.2,
                0.0,
                0.0,
                "BONE_MEAL",
                0
        ));

        List<String> fertilizers = commands.suggestFertilizerItems(null, CommandInput.empty());
        assertEquals(1, fertilizers.size());
        assertTrue(fertilizers.contains("croppy:basic_fertilizer"));
    }

    @Test
    @DisplayName("Verify addon IDs suggestion provider")
    void testSuggestAddonIds() {
        dev.darkblade.croppy.platform.paper.addon.PaperAddonManager manager =
                new dev.darkblade.croppy.platform.paper.addon.PaperAddonManager(null, null, java.nio.file.Path.of("addons"), null);
        dev.darkblade.croppy.api.addon.AddonDescription desc = new dev.darkblade.croppy.api.addon.AddonDescription(
                "orchard", "Orchard", "1.0", "orchard", java.util.Optional.empty(), List.of(), "", List.of(), List.of()
        );
        manager.registerDiscovered(desc, java.nio.file.Path.of("addons/orchard"));
        commands.setAddonManager(manager);

        List<String> suggestions = commands.suggestAddonIds(null, CommandInput.empty());
        assertTrue(suggestions.contains("all"));
        assertTrue(suggestions.contains("orchard"));
    }

    @Test
    @DisplayName("Verify /croppy inspect resolves target crop via CropTargetResolver")
    void testInspectRaycastUsesTargetResolver() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("wheat"),
                pos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );

        CropTargetResolver mockResolver = new CropTargetResolver(new CropManager()) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player) {
                return Optional.of(new TargetResult(pos, crop, TargetResult.TargetType.VIRTUAL_ENTITY));
            }
        };
        commands.setTargetResolver(mockResolver);

        AtomicReference<Component> sentMessage = new AtomicReference<>();
        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                        sentMessage.set((Component) args[0]);
                        return null;
                    }
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        commands.inspectRaycast(mockSource);
        assertNotNull(sentMessage.get(), "Expected inspect card or status to be sent to player");
    }

    @Test
    @DisplayName("Verify /croppy water updates lastWateredDay via CropTargetResolver")
    void testWaterTargetUsesTargetResolver() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("wheat"),
                pos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );

        CropTargetResolver mockResolver = new CropTargetResolver(new CropManager()) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player) {
                return Optional.of(new TargetResult(pos, crop, TargetResult.TargetType.VIRTUAL_ENTITY));
            }
        };
        commands.setTargetResolver(mockResolver);

        World mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getFullTime".equals(method.getName())) return 48000L;
                    if ("spawnParticle".equals(method.getName())) return null;
                    if ("getBlockAt".equals(method.getName())) {
                        return Proxy.newProxyInstance(
                                Block.class.getClassLoader(),
                                new Class<?>[]{Block.class},
                                (p, m, a) -> {
                                    if ("getType".equals(m.getName())) return org.bukkit.Material.DIRT;
                                    return null;
                                }
                        );
                    }
                    return null;
                }
        );

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    if ("playSound".equals(method.getName())) return null;
                    if ("sendMessage".equals(method.getName())) return null;
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        commands.waterTarget(mockSource);
        assertEquals(2L, crop.getLastWateredDay(), "Water command should update lastWateredDay to 48000 / 24000 = 2");
    }

    @Test
    @DisplayName("Verify /croppy fertilize updates fertilizer key via CropTargetResolver")
    void testFertilizeTargetUsesTargetResolver() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("wheat"),
                pos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );

        CropTargetResolver mockResolver = new CropTargetResolver(new CropManager()) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player) {
                return Optional.of(new TargetResult(pos, crop, TargetResult.TargetType.VIRTUAL_ENTITY));
            }
        };
        commands.setTargetResolver(mockResolver);

        World mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("spawnParticle".equals(method.getName())) return null;
                    return null;
                }
        );

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    if ("playSound".equals(method.getName())) return null;
                    if ("sendMessage".equals(method.getName())) return null;
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        Key fertilizerKey = Key.of("croppy", "basic_fertilizer");
        commands.fertilizeTarget(mockSource, fertilizerKey);
        assertEquals(fertilizerKey, crop.getFertilizerKey(), "Fertilize command should update crop's fertilizerKey");
    }

    @Test
    @DisplayName("Verify /croppy setprogress updates crop progress via CropTargetResolver")
    void testSetProgressUsesTargetResolver() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("wheat"),
                pos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );

        CropTargetResolver mockResolver = new CropTargetResolver(new CropManager()) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player) {
                return Optional.of(new TargetResult(pos, crop, TargetResult.TargetType.VIRTUAL_ENTITY));
            }
        };
        commands.setTargetResolver(mockResolver);

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    if ("sendMessage".equals(method.getName())) return null;
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        commands.setProgress(mockSource, 0.75);
        assertEquals(0.75, crop.getProgress(), 0.001, "Progress command should update crop progress to 0.75");
    }

    @Test
    @DisplayName("Verify /croppy setstage updates crop stage and synchronizes display via CropTargetResolver")
    void testSetStageUsesTargetResolver() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultCropInstance crop = new DefaultCropInstance(
                Key.croppy("wheat"),
                pos,
                0,
                0.5,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );

        CropTargetResolver mockResolver = new CropTargetResolver(new CropManager()) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player) {
                return Optional.of(new TargetResult(pos, crop, TargetResult.TargetType.VIRTUAL_ENTITY));
            }
        };
        commands.setTargetResolver(mockResolver);

        DefaultSpeciesRegistry speciesRegistry = new DefaultSpeciesRegistry();
        List<StageDefinition> stages = List.of(
                new StageDefinition("seed", 100.0, 0),
                new StageDefinition("growing", 100.0, 3),
                new StageDefinition("mature", 0.0, 7)
        );
        Species testSpecies = new Species() {
            private final Key key = Key.croppy("wheat");
            private final CropRequirements requirements = new CropRequirements(9, 1);
            @Override public Key getKey() { return key; }
            @Override public String getName() { return "Wheat"; }
            @Override public CropDisplay getDisplay() { return new CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds"); }
            @Override public List<StageDefinition> getStages() { return stages; }
            @Override public CropRequirements getRequirements() { return requirements; }
            @Override public HarvestDefinition getHarvest() { return null; }
            @Override public boolean isMature(int stageIndex) { return stageIndex >= 2; }
        };
        speciesRegistry.register(testSpecies);
        commands.setSpeciesRegistry(speciesRegistry);

        AtomicBoolean synced = new AtomicBoolean(false);
        AtomicReference<StageDefinition> syncedStage = new AtomicReference<>();
        commands.setDisplaySynchronizer((c, sp, st) -> {
            synced.set(true);
            syncedStage.set(st);
        });

        AtomicReference<Component> sentMessage = new AtomicReference<>();
        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                        sentMessage.set((Component) args[0]);
                        return null;
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        commands.setStage(mockSource, 2);
        assertEquals(2, crop.getStageIndex(), "SetStage command should update stageIndex to 2");
        assertEquals(0.0, crop.getProgress(), 0.001, "SetStage command should reset progress to 0.0");
        assertTrue(synced.get(), "DisplaySynchronizer should be invoked when changing stage");
        assertEquals("mature", syncedStage.get().id());
        assertNotNull(sentMessage.get(), "Expected confirmation message after stage update");
    }

    @Test
    @DisplayName("Verify /croppy inspect sends error when no target is resolved")
    void testInspectRaycastWhenNoTargetFound() {
        CropTargetResolver mockResolver = new CropTargetResolver(new CropManager()) {
            @Override
            public Optional<TargetResult> resolveTarget(Player player) {
                return Optional.empty();
            }
        };
        commands.setTargetResolver(mockResolver);

        AtomicReference<Component> sentMessage = new AtomicReference<>();
        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                        sentMessage.set((Component) args[0]);
                        return null;
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        commands.inspectRaycast(mockSource);
        assertNotNull(sentMessage.get(), "Expected no-crop-found error message to be sent to player");
    }

    @Test
    @DisplayName("Verify log_channels suggestion provider lists all LogChannel IDs")
    void testSuggestLogChannels() {
        List<String> suggestions = commands.suggestLogChannels(null, CommandInput.empty());
        assertEquals(LogChannel.values().length, suggestions.size());
        for (LogChannel channel : LogChannel.values()) {
            assertTrue(suggestions.contains(channel.id()), "Missing suggestion for " + channel.id());
        }
    }

    @Test
    @DisplayName("Verify /croppy debug subcommands manage PaperCroppyLogger runtime channels cleanly")
    void testDebugChannelManagementUnit() {
        List<String> consoleOutput = new ArrayList<>();
        PaperCroppyLogger logger = new PaperCroppyLogger((level, msg, throwable) ->
                consoleOutput.add(level.name() + ": " + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(msg))
        );
        logger.applyConfig(LogLevel.INFO, java.util.Set.of());
        commands.setCroppyLogger(logger);

        List<String> playerMessages = new ArrayList<>();
        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("locale".equals(method.getName())) return java.util.Locale.ENGLISH;
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof Component c) {
                        playerMessages.add(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c));
                        return null;
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if ("source".equals(method.getName())) return mockPlayer;
                    return null;
                }
        );

        commands.debugEnable(mockSource, "growth");
        assertTrue(logger.getDebugChannels().contains(LogChannel.GROWTH));

        // Verify debug logs go only to console sink, never forwarded to player chat
        logger.debug(LogChannel.GROWTH, () -> "CONSOLE_ONLY_GROWTH_TRACE");
        assertTrue(consoleOutput.stream().anyMatch(line -> line.contains("CONSOLE_ONLY_GROWTH_TRACE")));
        assertFalse(playerMessages.stream().anyMatch(line -> line.contains("CONSOLE_ONLY_GROWTH_TRACE")));

        commands.debugStatus(mockSource);
        assertTrue(playerMessages.stream().anyMatch(m -> m.contains("INFO") && m.contains("RICH")));

        commands.debugToggle(mockSource, "growth");
        assertFalse(logger.getDebugChannels().contains(LogChannel.GROWTH));

        commands.debugAll(mockSource);
        assertEquals(LogChannel.values().length, logger.getDebugChannels().size());

        commands.debugOff(mockSource);
        assertTrue(logger.getDebugChannels().isEmpty());

        playerMessages.clear();
        commands.debugToggle(mockSource, "nonexistent");
        assertTrue(playerMessages.stream().anyMatch(m -> m.contains("nonexistent") && m.contains("growth")));
    }
}
