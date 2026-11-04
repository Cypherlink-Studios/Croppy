package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseType;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.process.YamlProcessLoader;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import dev.darkblade.croppy.core.rule.DefaultRuleRegistry;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperProcessExecutionTest {

    private final YamlProcessLoader loader = new YamlProcessLoader();

    @Test
    @DisplayName("Should successfully load bundled tomato_soup_cook.yml resource")
    void shouldLoadBundledTomatoSoupCook() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("processes/tomato_soup_cook.yml");
        assertNotNull(stream, "processes/tomato_soup_cook.yml must exist in resources");

        ProcessDefinition def = loader.load(stream, "tomato_soup_cook.yml");
        assertEquals(Key.of("croppy", "tomato_soup_cook"), def.key());
        assertEquals("Tomato Soup Cooking", def.displayName());
        assertEquals(Key.of("croppy", "cooking_pot"), def.stationKey());
        assertFalse(def.phases().isEmpty());

        PhaseDefinition phase = def.phases().get(0);
        assertEquals(PhaseType.TIMED, phase.type());
        assertEquals(300L, phase.durationTicks()); // 15s = 300 ticks
        assertTrue(phase.pauseOnConditionFailure());
        assertNotNull(phase.display());
        assertNotNull(phase.effects());
        assertTrue(phase.effects().hasParticle());
        assertTrue(phase.effects().hasSound());
    }

    @Test
    @DisplayName("Should successfully load bundled vegetable_stew_cook.yml resource")
    void shouldLoadBundledVegetableStewCook() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("processes/vegetable_stew_cook.yml");
        assertNotNull(stream, "processes/vegetable_stew_cook.yml must exist in resources");

        ProcessDefinition def = loader.load(stream, "vegetable_stew_cook.yml");
        assertEquals(Key.of("croppy", "vegetable_stew_cook"), def.key());
        assertEquals("Vegetable Stew Cooking", def.displayName());
        assertEquals(Key.of("croppy", "cooking_pot"), def.stationKey());
        assertFalse(def.phases().isEmpty());

        PhaseDefinition phase = def.phases().get(0);
        assertEquals(500L, phase.durationTicks()); // 25s = 500 ticks
        assertTrue(phase.pauseOnConditionFailure());
    }

    @Test
    @DisplayName("Should successfully load bundled fried_egg_cook.yml resource")
    void shouldLoadBundledFriedEggCook() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("processes/fried_egg_cook.yml");
        assertNotNull(stream, "processes/fried_egg_cook.yml must exist in resources");

        ProcessDefinition def = loader.load(stream, "fried_egg_cook.yml");
        assertEquals(Key.of("croppy", "fried_egg_cook"), def.key());
        assertEquals("Fried Egg Cooking", def.displayName());
        assertEquals(Key.of("croppy", "skillet"), def.stationKey());
        assertFalse(def.phases().isEmpty());

        PhaseDefinition phase = def.phases().get(0);
        assertEquals(200L, phase.durationTicks()); // 10s = 200 ticks
        assertTrue(phase.pauseOnConditionFailure());
    }

    @Test
    @DisplayName("Should register all bundled default processes into DefaultProcessRegistry")
    void shouldRegisterAllBundledProcesses() {
        DefaultProcessRegistry registry = new DefaultProcessRegistry();

        String[] bundled = {
                "processes/tomato_soup_cook.yml",
                "processes/vegetable_stew_cook.yml",
                "processes/fried_egg_cook.yml"
        };

        for (String resourcePath : bundled) {
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
            assertNotNull(in, "Missing resource: " + resourcePath);
            ProcessDefinition def = loader.load(in, resourcePath);
            registry.register(def);
        }

        assertEquals(3, registry.values().size());
        assertTrue(registry.get(Key.of("croppy", "tomato_soup_cook")).isPresent());
        assertTrue(registry.get(Key.of("croppy", "vegetable_stew_cook")).isPresent());
        assertTrue(registry.get(Key.of("croppy", "fried_egg_cook")).isPresent());
    }

    @Test
    @DisplayName("PaperStationTicker auto-starts cooking on heated idle skillet containing recipe inputs")
    void testIdleSkilletAutoStartsProcessWhenHeated() {
        Plugin plugin = (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("isEnabled".equals(method.getName())) return false;
                    if ("getName".equals(method.getName())) return "Croppy";
                    return null;
                }
        );

        DefaultProcessRegistry processRegistry = new DefaultProcessRegistry();
        InputStream procIn = getClass().getClassLoader().getResourceAsStream("processes/fried_egg_cook.yml");
        assertNotNull(procIn);
        ProcessDefinition procDef = loader.load(procIn, "fried_egg_cook.yml");
        processRegistry.register(procDef);

        DefaultRecipeRegistry recipeRegistry = new DefaultRecipeRegistry();
        Key skilletKey = Key.of("croppy", "skillet");
        Key procKey = Key.of("croppy", "fried_egg_cook");
        Key eggKey = Key.of("minecraft", "egg");
        Key friedEggKey = Key.of("croppy", "fried_egg");

        RecipeDefinition recipe = new RecipeDefinition(
                friedEggKey, "Fried Egg", Key.of("croppy", "fried"), skilletKey, procKey,
                List.of(StoredItem.of(eggKey, 1)),
                friedEggKey, 1, null, true, List.of(), Map.of()
        );
        recipeRegistry.register(recipe);

        ProcessEngine processEngine = new ProcessEngine(processRegistry, recipeRegistry);
        DefaultStationManager stationManager = new DefaultStationManager();
        RuleEngine ruleEngine = new RuleEngine(new DefaultRuleRegistry());
        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());

        PaperStationTicker ticker = new PaperStationTicker(
                plugin, stationManager, processEngine, new PaperThermalEvaluator(),
                ruleEngine, null, itemFactory, null, null, 20L
        );

        StationDefinition skilletDef = new StationDefinition(
                skilletKey, "Skillet", "minecraft:heavy_weighted_pressure_plate", StationInteractionType.PHYSICAL,
                4, 2, true, List.of(), Map.of("thermal", "true")
        );
        DefaultStationInstance station = new DefaultStationInstance(skilletKey, BlockPosition.of("world", 0, 64, 0), skilletDef);
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addInput(StoredItem.of(eggKey, 1));
        station.setComponent(StorageComponent.class, storage);
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent());
        // Set skillet to 140°C (satisfies start_conditions: station.temperature >= 120)
        station.setComponent(ThermalComponent.class, new DefaultThermalComponent(140.0, 140.0, true));

        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "world";
                    if ("getTime".equals(method.getName())) return 1000L;
                    if ("isChunkLoaded".equals(method.getName())) return true;
                    if ("getBlockAt".equals(method.getName())) {
                        return Proxy.newProxyInstance(
                                Block.class.getClassLoader(),
                                new Class<?>[]{Block.class},
                                (bProxy, bMethod, bArgs) -> {
                                    if ("getLightLevel".equals(bMethod.getName())) return (byte) 15;
                                    if ("getType".equals(bMethod.getName())) return org.bukkit.Material.CAMPFIRE;
                                    return null;
                                }
                        );
                    }
                    if ("getPlayers".equals(method.getName())) return List.of();
                    return null;
                }
        );

        // 1. Tick station while idle but heated and containing inputs
        ticker.tickStation(station, world);

        // 2. Verify process automatically started!
        assertTrue(storage.getInputs().isEmpty(), "Egg input must be consumed upon auto-starting");
        ProcessComponent procComp = station.getComponent(ProcessComponent.class).orElseThrow();
        assertTrue(procComp.hasActiveProcess(), "Skillet must have active process after ticker execution");
        assertEquals(ProcessState.RUNNING, procComp.getActiveProcess().get().getState());
        assertEquals(procKey, procComp.getActiveProcess().get().getProcessKey());
    }
}
