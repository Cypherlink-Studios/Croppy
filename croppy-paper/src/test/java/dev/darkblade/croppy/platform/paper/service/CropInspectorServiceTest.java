package dev.darkblade.croppy.platform.paper.service;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.EnvironmentProvider;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CropInspectorServiceTest {

    private CropManager cropManager;
    private DefaultSpeciesRegistry speciesRegistry;
    private CropInspectorService inspectorService;
    private Species testSpecies;

    @BeforeEach
    void setUp() {
        cropManager = new CropManager();
        speciesRegistry = new DefaultSpeciesRegistry();
        EnvironmentProvider envProvider = pos -> CropEnvironment.of(12, 8);
        inspectorService = new CropInspectorService(cropManager, speciesRegistry, envProvider);

        testSpecies = new Species() {
            private final Key key = Key.croppy("golden_wheat");
            private final List<StageDefinition> stages = List.of(
                    new StageDefinition("seed", 100.0, 0),
                    new StageDefinition("growing", 100.0, 3),
                    new StageDefinition("mature", 0.0, 7)
            );
            private final CropRequirements requirements = new CropRequirements(9, 1);

            @Override
            public Key getKey() { return key; }

            @Override
            public String getName() { return "Golden Wheat"; }

            @Override
            public CropDisplay getDisplay() {
                return new CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds");
            }

            @Override
            public List<StageDefinition> getStages() { return stages; }

            @Override
            public CropRequirements getRequirements() { return requirements; }

            @Override
            public HarvestDefinition getHarvest() { return null; }
        };

        speciesRegistry.register(testSpecies);
    }

    @Test
    void testProgressBarMath() {
        String zero = CropInspectorService.renderProgressBar(0.0, 10);
        assertTrue(zero.contains("□□□□□□□□□□"));

        String half = CropInspectorService.renderProgressBar(0.5, 10);
        assertTrue(half.contains("■■■■■"));
        assertTrue(half.contains("□□□□□"));

        String full = CropInspectorService.renderProgressBar(1.0, 10);
        assertTrue(full.contains("■■■■■■■■■■"));

        // Clamping bounds
        String clampedNegative = CropInspectorService.renderProgressBar(-0.2, 10);
        assertTrue(clampedNegative.contains("□□□□□□□□□□"));

        String clampedOver = CropInspectorService.renderProgressBar(1.5, 10);
        assertTrue(clampedOver.contains("■■■■■■■■■■"));
    }

    @Test
    void testInspectReturnsEmptyWhenNoCrop() {
        BlockPosition pos = new BlockPosition("world", 10, 64, 10);
        Optional<Component> result = inspectorService.inspect(pos);
        assertTrue(result.isEmpty());
    }

    @Test
    void testInspectReturnsCardWhenCropExists() {
        BlockPosition pos = new BlockPosition("world", 10, 64, 10);
        CropInstance crop = new DefaultCropInstance(testSpecies.getKey(), pos);
        crop.setProgress(50.0);
        cropManager.registerCrop(crop);

        Optional<Component> result = inspectorService.inspect(pos);
        assertTrue(result.isPresent());

        String plainText = PlainTextComponentSerializer.plainText().serialize(result.get());
        assertTrue(plainText.contains("Crop Inspection"));
        assertTrue(plainText.contains("Golden Wheat"));
        assertTrue(plainText.contains("croppy:golden_wheat"));
        assertTrue(plainText.contains("Stage: 1/3"));
        assertTrue(plainText.contains("50.0%"));
        assertTrue(plainText.contains("Growing"));
    }

    @Test
    void testFormatCropCardMatureState() {
        BlockPosition pos = new BlockPosition("world", 20, 64, 20);
        CropInstance crop = new DefaultCropInstance(testSpecies.getKey(), pos, 2, 0.0, 1000L, 1000L);
        CropEnvironment env = CropEnvironment.of(15, 8);

        Component card = inspectorService.formatCropCard(crop, testSpecies, env);
        String plainText = PlainTextComponentSerializer.plainText().serialize(card);

        assertTrue(plainText.contains("Stage: 3/3"));
        assertTrue(plainText.contains("100.0%"));
        assertTrue(plainText.contains("Mature"));
    }

    @Test
    void testFormatCropCardStalledConditions() {
        BlockPosition pos = new BlockPosition("world", 30, 64, 30);
        CropInstance crop = new DefaultCropInstance(testSpecies.getKey(), pos);

        // Insufficient light (5 < 9)
        CropEnvironment lowLight = CropEnvironment.of(5, 8);
        Component cardLowLight = inspectorService.formatCropCard(crop, testSpecies, lowLight);
        String textLowLight = PlainTextComponentSerializer.plainText().serialize(cardLowLight);
        assertTrue(textLowLight.contains("Insufficient Light"));

        // Insufficient water (0 < 1)
        CropEnvironment noWater = CropEnvironment.of(12, 0);
        Component cardNoWater = inspectorService.formatCropCard(crop, testSpecies, noWater);
        String textNoWater = PlainTextComponentSerializer.plainText().serialize(cardNoWater);
        assertTrue(textNoWater.contains("Insufficient Water"));
    }
}
