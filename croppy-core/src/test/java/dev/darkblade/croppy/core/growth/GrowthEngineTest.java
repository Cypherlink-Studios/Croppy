package dev.darkblade.croppy.core.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GrowthEngineTest {

    private GrowthEngine engine;
    private Species wheatSpecies;
    private Species dryCactusSpecies;
    private BlockPosition position;

    @BeforeEach
    void setUp() {
        engine = new GrowthEngine(1.0);
        position = BlockPosition.of("world", 10, 64, 20);

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        String wheatYaml = """
                id: "croppy:wheat"
                name: "Trigo"
                display:
                  type: "vanilla"
                  block: "minecraft:wheat"
                  seed_item: "minecraft:wheat_seeds"
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
                      block_age: 4
                  - id: "mature"
                    progress_required: 0
                    display:
                      block_age: 7
                harvest:
                  drops:
                    - item: "minecraft:wheat"
                      min: 1
                      max: 1
                """;
        wheatSpecies = loader.load(new ByteArrayInputStream(wheatYaml.getBytes(StandardCharsets.UTF_8)), "wheat.yml");

        String cactusYaml = """
                id: "croppy:cactus"
                name: "Cactus"
                display:
                  type: "vanilla"
                  block: "minecraft:cactus"
                  seed_item: "minecraft:cactus"
                requirements:
                  min_light: 0
                  water_fluid_level: 0
                stages:
                  - id: "stage_0"
                    progress_required: 10.0
                    display:
                      block_age: 0
                  - id: "mature"
                    progress_required: 0
                    display:
                      block_age: 1
                harvest:
                  drops:
                    - item: "minecraft:cactus"
                      min: 1
                      max: 1
                """;
        dryCactusSpecies = loader.load(new ByteArrayInputStream(cactusYaml.getBytes(StandardCharsets.UTF_8)), "cactus.yml");
    }

    @Test
    @DisplayName("Unloaded environment skips growth evaluation")
    void testUnloadedEnvironmentSkips() {
        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.unloaded());

        assertFalse(result.evaluated());
        assertFalse(result.requirementsMet());
        assertEquals(0.0, result.progressAdded());
        assertEquals(0.0, crop.getProgress());
    }

    @Test
    @DisplayName("Mature crop skips growth evaluation")
    void testMatureCropSkips() {
        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        crop.setStageIndex(2); // mature stage
        assertTrue(crop.isMature(wheatSpecies));

        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8));
        assertFalse(result.evaluated());
        assertEquals(0.0, result.progressAdded());
    }

    @Test
    @DisplayName("Light level below requirement halts growth")
    void testLightRequirementThreshold() {
        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);

        // Required light is 9; test with light 8
        GrowthResult insufficient = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(8, 8));
        assertTrue(insufficient.evaluated());
        assertFalse(insufficient.requirementsMet());
        assertEquals(0.0, insufficient.progressAdded());
        assertEquals(0.0, crop.getProgress());

        // Test with exact required light 9
        GrowthResult exact = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(9, 8));
        assertTrue(exact.evaluated());
        assertTrue(exact.requirementsMet());
        assertEquals(1.0, exact.progressAdded());
        assertEquals(1.0, crop.getProgress());
    }

    @Test
    @DisplayName("Water fluid level below requirement halts growth")
    void testWaterRequirementThreshold() {
        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);

        // Required water fluid level is 8 (source); test with flowing water level 7
        GrowthResult insufficient = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(12, 7));
        assertTrue(insufficient.evaluated());
        assertFalse(insufficient.requirementsMet());
        assertEquals(0.0, insufficient.progressAdded());
        assertEquals(0.0, crop.getProgress());

        // Test with source water (level 8)
        GrowthResult satisfied = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(12, 8));
        assertTrue(satisfied.evaluated());
        assertTrue(satisfied.requirementsMet());
        assertEquals(1.0, satisfied.progressAdded());
        assertEquals(1.0, crop.getProgress());
    }

    @Test
    @DisplayName("Dry-farming crop grows with zero water")
    void testDryFarmingCropGrowsWithoutWater() {
        CropInstance crop = new DefaultCropInstance(dryCactusSpecies.getKey(), position);

        GrowthResult result = engine.processCrop(crop, dryCactusSpecies, CropEnvironment.of(5, 0));
        assertTrue(result.evaluated());
        assertTrue(result.requirementsMet());
        assertEquals(1.0, result.progressAdded());
    }

    @Test
    @DisplayName("Growth modifier pipeline multiplies progress correctly")
    void testModifierMultiplicationPipeline() {
        engine.setBaseProgress(2.0);
        engine.addModifier(ctx -> 1.5);
        engine.addModifier(ctx -> 2.0);

        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8));

        assertTrue(result.requirementsMet());
        assertEquals(6.0, result.progressAdded(), 0.001); // 2.0 * 1.5 * 2.0 = 6.0
        assertEquals(6.0, crop.getProgress(), 0.001);
    }

    @Test
    @DisplayName("Crop advances stage when progress threshold is reached")
    void testStageAdvancementOnThreshold() {
        engine.setBaseProgress(5.0);
        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);

        // Cycle 1: 5.0 progress / 10.0 needed
        GrowthResult r1 = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8));
        assertFalse(r1.stageAdvanced());
        assertEquals(0, crop.getStageIndex());
        assertEquals(5.0, crop.getProgress());

        // Cycle 2: another 5.0 progress -> 10.0 total -> advances to stage 1 and resets progress
        GrowthResult r2 = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8));
        assertTrue(r2.stageAdvanced());
        assertEquals(1, crop.getStageIndex());
        assertEquals(0.0, crop.getProgress());
    }

    @Test
    @DisplayName("Stage advance interceptor can cancel stage transition and preserve state safely")
    void testInterceptorCanVetoStageAdvancement() {
        engine.setBaseProgress(10.0);
        AtomicBoolean vetoed = new AtomicBoolean(false);
        engine.setStageAdvanceInterceptor((c, s, from, to) -> {
            vetoed.set(true);
            return false; // veto
        });

        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8));

        assertTrue(vetoed.get());
        assertFalse(result.stageAdvanced());
        assertEquals(0, crop.getStageIndex());
        assertEquals(10.0, crop.getProgress()); // capped safely
    }

    @Test
    @DisplayName("Display synchronizer is notified upon successful stage advance")
    void testDisplaySynchronizerInvokedOnAdvance() {
        engine.setBaseProgress(10.0);
        AtomicReference<String> syncedStageId = new AtomicReference<>();
        engine.setDisplaySynchronizer((c, s, stage) -> syncedStageId.set(stage.id()));

        CropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8));

        assertTrue(result.stageAdvanced());
        assertEquals(1, crop.getStageIndex());
        assertEquals("stage_1", syncedStageId.get());
    }

    @Test
    @DisplayName("FertilizerGrowthModifier multiplies base progress by speed multiplier")
    void testFertilizerGrowthModifier() {
        dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry registry = new dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry();
        dev.darkblade.croppy.api.identifier.Key speedKey = dev.darkblade.croppy.api.identifier.Key.of("croppy", "growth_fertilizer");
        registry.register(new dev.darkblade.croppy.api.fertilizer.Fertilizer(
                speedKey, "Speed", java.util.List.of(), dev.darkblade.croppy.api.fertilizer.FertilizerType.SPEED,
                1.5, 0.0, 0.0, "BONE_MEAL", 0
        ));

        engine.addModifier(new FertilizerGrowthModifier(registry));
        engine.setBaseProgress(2.0);

        DefaultCropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        crop.setFertilizerKey(speedKey);
        crop.setLastWateredDay(1);

        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8), 1);
        assertEquals(3.0, result.progressAdded(), 0.001);
        assertEquals(3.0, crop.getProgress(), 0.001);
    }

    @Test
    @DisplayName("Unwatered crop with PAUSE policy accumulates zero progress")
    void testUnwateredPolicyPause() {
        engine.setBaseProgress(5.0);
        DefaultCropInstance crop = new DefaultCropInstance(wheatSpecies.getKey(), position);
        crop.setLastWateredDay(0); // watered on day 0, today is day 1

        GrowthResult result = engine.processCrop(crop, wheatSpecies, CropEnvironment.of(15, 8), 1);
        assertEquals(0.0, result.progressAdded(), 0.001);
        assertEquals(0.0, crop.getProgress(), 0.001);
    }

    @Test
    @DisplayName("Unwatered crop with SLOW policy accumulates reduced progress")
    void testUnwateredPolicySlow() {
        engine.setBaseProgress(10.0);
        Species slowSpecies = new Species() {
            @Override public dev.darkblade.croppy.api.identifier.Key getKey() { return wheatSpecies.getKey(); }
            @Override public String getName() { return wheatSpecies.getName(); }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() { return wheatSpecies.getDisplay(); }
            @Override public java.util.List<dev.darkblade.croppy.api.species.StageDefinition> getStages() { return wheatSpecies.getStages(); }
            @Override public dev.darkblade.croppy.api.species.CropRequirements getRequirements() { return wheatSpecies.getRequirements(); }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return wheatSpecies.getHarvest(); }
            @Override public dev.darkblade.croppy.api.growth.UnwateredPolicy getUnwateredPolicy() {
                return dev.darkblade.croppy.api.growth.UnwateredPolicy.slow(0.2);
            }
        };

        DefaultCropInstance crop = new DefaultCropInstance(slowSpecies.getKey(), position);
        crop.setLastWateredDay(0);

        GrowthResult result = engine.processCrop(crop, slowSpecies, CropEnvironment.of(15, 8), 1);
        assertEquals(2.0, result.progressAdded(), 0.001);
        assertEquals(2.0, crop.getProgress(), 0.001);
    }

    @Test
    @DisplayName("Unwatered crop with DECAY_PROGRESS loses progress")
    void testUnwateredPolicyDecay() {
        Species decaySpecies = new Species() {
            @Override public dev.darkblade.croppy.api.identifier.Key getKey() { return wheatSpecies.getKey(); }
            @Override public String getName() { return wheatSpecies.getName(); }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() { return wheatSpecies.getDisplay(); }
            @Override public java.util.List<dev.darkblade.croppy.api.species.StageDefinition> getStages() { return wheatSpecies.getStages(); }
            @Override public dev.darkblade.croppy.api.species.CropRequirements getRequirements() { return wheatSpecies.getRequirements(); }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return wheatSpecies.getHarvest(); }
            @Override public dev.darkblade.croppy.api.growth.UnwateredPolicy getUnwateredPolicy() {
                return dev.darkblade.croppy.api.growth.UnwateredPolicy.decay(3.0);
            }
        };

        DefaultCropInstance crop = new DefaultCropInstance(decaySpecies.getKey(), position);
        crop.setProgress(8.0);
        crop.setLastWateredDay(0);

        GrowthResult result = engine.processCrop(crop, decaySpecies, CropEnvironment.of(15, 8), 1);
        assertEquals(-3.0, result.progressAdded(), 0.001);
        assertEquals(5.0, crop.getProgress(), 0.001);
    }

    @Test
    @DisplayName("Unwatered crop with WITHER policy triggers withering after exceeding maxDryDays")
    void testUnwateredPolicyWither() {
        Species witherSpecies = new Species() {
            @Override public dev.darkblade.croppy.api.identifier.Key getKey() { return wheatSpecies.getKey(); }
            @Override public String getName() { return wheatSpecies.getName(); }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() { return wheatSpecies.getDisplay(); }
            @Override public java.util.List<dev.darkblade.croppy.api.species.StageDefinition> getStages() { return wheatSpecies.getStages(); }
            @Override public dev.darkblade.croppy.api.species.CropRequirements getRequirements() { return wheatSpecies.getRequirements(); }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return wheatSpecies.getHarvest(); }
            @Override public dev.darkblade.croppy.api.growth.UnwateredPolicy getUnwateredPolicy() {
                return dev.darkblade.croppy.api.growth.UnwateredPolicy.wither(2);
            }
        };

        AtomicBoolean witheredCalled = new AtomicBoolean(false);
        engine.setWitherHandler((c, s) -> witheredCalled.set(true));

        DefaultCropInstance crop = new DefaultCropInstance(witherSpecies.getKey(), position);
        crop.setLastWateredDay(0);

        // Day 1: 1 dry day (<= 2), pauses
        GrowthResult r1 = engine.processCrop(crop, witherSpecies, CropEnvironment.of(15, 8), 1);
        assertFalse(r1.withered());
        assertFalse(witheredCalled.get());

        // Day 2: 2 dry days (<= 2), pauses
        GrowthResult r2 = engine.processCrop(crop, witherSpecies, CropEnvironment.of(15, 8), 2);
        assertFalse(r2.withered());
        assertFalse(witheredCalled.get());

        // Day 3: 3 dry days (> 2), withers!
        GrowthResult r3 = engine.processCrop(crop, witherSpecies, CropEnvironment.of(15, 8), 3);
        assertTrue(r3.withered());
        assertTrue(witheredCalled.get());
    }
}
