package dev.darkblade.croppy.integration.papi;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CroppyPlaceholderExpansion Unit Tests")
class CroppyPlaceholderExpansionTest {

    private CroppyCore core;
    private CroppyPlaceholderExpansion expansion;

    @BeforeEach
    void setUp() {
        core = new CroppyCore(Logger.getLogger("test"));
        core.initialize();
        expansion = new CroppyPlaceholderExpansion(core, "0.5.4", 5);
    }

    @Test
    @DisplayName("Expansion metadata matches expected values")
    void testExpansionMetadata() {
        assertEquals("croppy", expansion.getIdentifier());
        assertEquals("DarkBladeDev", expansion.getAuthor());
        assertEquals("0.5.4", expansion.getVersion());
        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
    }

    @Test
    @DisplayName("Global placeholders resolve accurately")
    void testGlobalPlaceholders() {
        assertEquals("0.5.4", expansion.onRequest(null, "version"));
        assertEquals("0", expansion.onRequest(null, "total_active_crops"));
        assertEquals("0", expansion.onRequest(null, "total_active_stations"));
        assertNull(expansion.onRequest(null, "non_existent_placeholder"));
    }

    @Test
    @DisplayName("Compendium placeholders support aliases and progression telemetry")
    void testCompendiumAliases() {
        UUID playerUuid = UUID.randomUUID();
        OfflinePlayer mockPlayer = createMockOfflinePlayer(playerUuid);

        // Before recipes registered
        assertEquals("0", expansion.onRequest(mockPlayer, "compendium_total"));
        assertEquals("0", expansion.onRequest(mockPlayer, "compendium_total_recipes"));
        assertEquals("0", expansion.onRequest(mockPlayer, "compendium_discovered"));
        assertEquals("0", expansion.onRequest(mockPlayer, "compendium_discovered_recipes"));
        assertEquals("0.0%", expansion.onRequest(mockPlayer, "compendium_percentage"));
        assertEquals("0.0%", expansion.onRequest(mockPlayer, "compendium_completion_percentage"));

        // Register recipe
        Key recipeKey = Key.of("croppy", "omelet");
        RecipeDefinition recipe = new RecipeDefinition(
                recipeKey,
                "Omelet",
                Key.of("croppy", "meal"),
                Key.of("croppy", "skillet"),
                Key.of("croppy", "omelet_cook"),
                List.of(),
                Key.of("croppy", "cooked_omelet"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        core.getRecipeRegistry().register(recipe);

        assertEquals("1", expansion.onRequest(mockPlayer, "compendium_total"));
        assertEquals("1", expansion.onRequest(mockPlayer, "compendium_total_recipes"));
        assertEquals("0", expansion.onRequest(mockPlayer, "compendium_discovered"));
        assertEquals("0", expansion.onRequest(mockPlayer, "compendium_discovered_recipes"));
        assertEquals("0.0%", expansion.onRequest(mockPlayer, "compendium_percentage"));
        assertEquals("0.0%", expansion.onRequest(mockPlayer, "compendium_completion_percentage"));
        assertEquals("false", expansion.onRequest(mockPlayer, "compendium_is_unlocked_croppy:omelet"));

        // Discover recipe
        core.getRecipeDiscoveryEngine().discoverRecipe(playerUuid, recipe, Key.of("croppy", "manual"));

        assertEquals("1", expansion.onRequest(mockPlayer, "compendium_discovered"));
        assertEquals("1", expansion.onRequest(mockPlayer, "compendium_discovered_recipes"));
        assertEquals("100.0%", expansion.onRequest(mockPlayer, "compendium_percentage"));
        assertEquals("100.0%", expansion.onRequest(mockPlayer, "compendium_completion_percentage"));
        assertEquals("true", expansion.onRequest(mockPlayer, "compendium_is_unlocked_croppy:omelet"));
    }

    @Test
    @DisplayName("Target crop telemetry resolves canonical key, display name, stage indices and progress")
    void testTargetCropTelemetry() {
        String yaml = """
                id: "croppy:grapes"
                name: "<dark_purple>Sweet Grapes</dark_purple>"
                display:
                  type: "vanilla"
                  block: "minecraft:oak_leaves"
                  seed_item: "croppy:grape_seeds"
                requirements:
                  min_light: 9
                  water_fluid_level: 8
                stages:
                  - id: "stage_0"
                    progress_required: 10.0
                    display:
                      block_age: 0
                  - id: "stage_1"
                    progress_required: 10.0
                    display:
                      block_age: 1
                  - id: "stage_2"
                    progress_required: 10.0
                    display:
                      block_age: 2
                  - id: "stage_3"
                    progress_required: 10.0
                    display:
                      block_age: 3
                  - id: "stage_4"
                    progress_required: 0
                    display:
                      block_age: 4
                harvest:
                  drops:
                    - item: "croppy:grapes"
                      min: 1
                      max: 2
                """;
        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        Species species = loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "grapes.yml");
        core.getSpeciesRegistry().register(species);

        BlockPosition pos = new BlockPosition("world", 10, 64, 10);
        CropInstance crop = new DefaultCropInstance(species.getKey(), pos, 3, 0.745, 0L, 0L, 1L, Key.of("croppy", "growth_fertilizer"), 0.0f);
        core.getCropManager().registerCrop(crop);

        World mockWorld = createMockWorld("world", 24000L); // Day 1
        Block mockBlock = createMockBlock(mockWorld, 10, 64, 10, null, null);
        Player mockPlayer = createMockPlayer(UUID.randomUUID(), mockBlock);

        assertEquals("true", expansion.onRequest(mockPlayer, "target_is_crop"));
        assertEquals("croppy:grapes", expansion.onRequest(mockPlayer, "target_crop_species"));
        assertEquals("<dark_purple>Sweet Grapes</dark_purple>", expansion.onRequest(mockPlayer, "target_crop_species_name"));
        assertEquals("3", expansion.onRequest(mockPlayer, "target_crop_stage"));
        assertEquals("4", expansion.onRequest(mockPlayer, "target_crop_max_stage"));
        assertEquals("5", expansion.onRequest(mockPlayer, "target_crop_total_stages"));
        assertEquals("74.5%", expansion.onRequest(mockPlayer, "target_crop_progress"));
        assertEquals("true", expansion.onRequest(mockPlayer, "target_crop_is_watered"));
        assertEquals("croppy:growth_fertilizer", expansion.onRequest(mockPlayer, "target_crop_fertilizer"));
    }

    @Test
    @DisplayName("Target station telemetry resolves aliases, temperature and multi-phase cooking progress")
    void testTargetStationTelemetry() {
        Key stationKey = Key.of("croppy", "skillet");
        StationDefinition stationDef = new StationDefinition(
                stationKey,
                "Skillet",
                "minecraft:iron_block",
                StationInteractionType.HYBRID,
                2,
                1,
                true,
                List.of(),
                Map.of()
        );
        core.getStationRegistry().register(stationDef);

        Key processKey = Key.of("croppy", "fried_egg_cook");
        ProcessDefinition processDef = new ProcessDefinition(
                processKey,
                "Fried Egg",
                stationKey,
                List.of(),
                null,
                List.of(
                        PhaseDefinition.timed("sizzle", 100, null),
                        PhaseDefinition.timed("flip", 100, null)
                ),
                null,
                Key.of("croppy", "fried_egg"),
                1,
                Map.of()
        );
        core.getProcessRegistry().register(processDef);

        BlockPosition pos = new BlockPosition("world", 10, 64, 10);
        DefaultStationInstance station = new DefaultStationInstance(stationKey, pos, stationDef);
        station.setComponent(ThermalComponent.class, new DefaultThermalComponent(185.0, 185.0, true));
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent());
        core.getStationManager().addStation(station);

        World mockWorld = createMockWorld("world", 1000L);
        Block mockBlock = createMockBlock(mockWorld, 10, 64, 10, null, null);
        Player mockPlayer = createMockPlayer(UUID.randomUUID(), mockBlock);

        assertEquals("true", expansion.onRequest(mockPlayer, "target_is_station"));
        assertEquals("croppy:skillet", expansion.onRequest(mockPlayer, "target_station_name"));
        assertEquals("croppy:skillet", expansion.onRequest(mockPlayer, "target_station_type"));
        assertEquals("185.0°C", expansion.onRequest(mockPlayer, "target_station_temperature"));
        assertEquals("false", expansion.onRequest(mockPlayer, "target_station_is_cooking"));
        assertEquals("0.0%", expansion.onRequest(mockPlayer, "target_station_cooking_progress"));

        // Activate cooking process: 124 ticks elapsed out of 200 total ticks = 62.0%
        DefaultProcessInstance process = new DefaultProcessInstance(processKey, ProcessState.RUNNING, 1, 24, 124);
        station.getComponent(ProcessComponent.class).ifPresent(pc -> pc.setActiveProcess(process));

        assertEquals("true", expansion.onRequest(mockPlayer, "target_station_is_cooking"));
        assertEquals("62.0%", expansion.onRequest(mockPlayer, "target_station_cooking_progress"));
    }

    @Test
    @DisplayName("Target soil telemetry calculates moisture and water level for direct and crop sightlines")
    void testTargetSoilMoisture() {
        Farmland mockFarmland = createMockFarmland(6);
        World mockWorld = createMockWorld("world", 1000L);

        // Case 1: Direct Farmland targeting
        Block directFarmlandBlock = createMockBlock(mockWorld, 10, 63, 10, mockFarmland, null);
        Player playerAimingFarmland = createMockPlayer(UUID.randomUUID(), directFarmlandBlock);

        assertEquals("true", expansion.onRequest(playerAimingFarmland, "target_soil_is_farmland"));
        assertEquals("6", expansion.onRequest(playerAimingFarmland, "target_soil_water"));
        assertEquals("86%", expansion.onRequest(playerAimingFarmland, "target_soil_moisture"));

        // Case 2: Crop targeting with farmland below
        Block cropBlock = createMockBlock(mockWorld, 10, 64, 10, null, directFarmlandBlock);
        Player playerAimingCrop = createMockPlayer(UUID.randomUUID(), cropBlock);

        assertEquals("true", expansion.onRequest(playerAimingCrop, "target_soil_is_farmland"));
        assertEquals("6", expansion.onRequest(playerAimingCrop, "target_soil_water"));
        assertEquals("86%", expansion.onRequest(playerAimingCrop, "target_soil_moisture"));

        // Case 3: Stone / Non-Farmland
        Block stoneBlock = createMockBlock(mockWorld, 10, 64, 10, null, null);
        Player playerAimingStone = createMockPlayer(UUID.randomUUID(), stoneBlock);

        assertEquals("false", expansion.onRequest(playerAimingStone, "target_soil_is_farmland"));
        assertEquals("0", expansion.onRequest(playerAimingStone, "target_soil_water"));
        assertEquals("0%", expansion.onRequest(playerAimingStone, "target_soil_moisture"));
    }

    private OfflinePlayer createMockOfflinePlayer(UUID uuid) {
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return uuid;
                    return null;
                }
        );
    }

    private Player createMockPlayer(UUID uuid, Block targetBlock) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return uuid;
                    if ("getTargetBlockExact".equals(method.getName())) return targetBlock;
                    return null;
                }
        );
    }

    private Block createMockBlock(World world, int x, int y, int z, Object blockData, Block below) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return world;
                    if ("getX".equals(method.getName())) return x;
                    if ("getY".equals(method.getName())) return y;
                    if ("getZ".equals(method.getName())) return z;
                    if ("getBlockData".equals(method.getName())) return blockData;
                    if ("getRelative".equals(method.getName())) return below;
                    return null;
                }
        );
    }

    private World createMockWorld(String name, long fullTime) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return name;
                    if ("getFullTime".equals(method.getName())) return fullTime;
                    return null;
                }
        );
    }

    private Farmland createMockFarmland(int moisture) {
        return (Farmland) Proxy.newProxyInstance(
                Farmland.class.getClassLoader(),
                new Class<?>[]{Farmland.class},
                (proxy, method, args) -> {
                    if ("getMoisture".equals(method.getName())) return moisture;
                    return null;
                }
        );
    }
}
