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

    @Test
    void testFormatCropCardHydrationAndFertilizer() {
        BlockPosition pos = new BlockPosition("world", 40, 64, 40);
        DefaultCropInstance crop = new DefaultCropInstance(testSpecies.getKey(), pos);
        crop.setLastWateredDay(5L);
        crop.setFertilizerKey(Key.of("croppy", "speed_gro"));

        CropEnvironment env = CropEnvironment.of(15, 8);

        // When currentDay is 5 (watered today)
        Component cardWatered = inspectorService.formatCropCard(crop, testSpecies, env, 5L);
        String textWatered = PlainTextComponentSerializer.plainText().serialize(cardWatered);
        assertTrue(textWatered.contains("Watered today"));
        assertTrue(textWatered.contains("Fertilizer: croppy:speed_gro"));
        assertTrue(textWatered.contains("Unwatered Policy: PAUSE"));

        // When currentDay is 7 (dry for 2 days)
        Component cardDry = inspectorService.formatCropCard(crop, testSpecies, env, 7L);
        String textDry = PlainTextComponentSerializer.plainText().serialize(cardDry);
        assertTrue(textDry.contains("Dry (2 day(s) ago)"));
    }

    @Test
    void testFormatCropCardLocalizedWithService() {
        dev.darkblade.croppy.core.i18n.DefaultMessageCatalog catalog = new dev.darkblade.croppy.core.i18n.DefaultMessageCatalog(
                dev.darkblade.croppy.api.i18n.LocaleRef.ES_ES, dev.darkblade.croppy.api.i18n.LocaleRef.EN_US
        );
        dev.darkblade.croppy.core.i18n.YamlLocaleLoader loader = new dev.darkblade.croppy.core.i18n.YamlLocaleLoader();
        try (var esIn = getClass().getResourceAsStream("/locales/es-ES.yml")) {
            assertNotNull(esIn);
            catalog.registerLocale(dev.darkblade.croppy.api.i18n.LocaleRef.ES_ES, loader.load(esIn, "es-ES.yml"));
        } catch (Exception e) {
            fail(e);
        }

        dev.darkblade.croppy.platform.paper.i18n.PaperMessageService messageService =
                new dev.darkblade.croppy.platform.paper.i18n.PaperMessageService(catalog, null);

        CropInspectorService localizedInspector = new CropInspectorService(
                cropManager, speciesRegistry, pos -> CropEnvironment.of(12, 8), null, null, messageService
        );

        BlockPosition pos = new BlockPosition("world", 45, 64, 45);
        DefaultCropInstance crop = new DefaultCropInstance(testSpecies.getKey(), pos);
        crop.setLastWateredDay(5L);

        Component card = localizedInspector.formatCropCard(crop, testSpecies, CropEnvironment.of(15, 8), 5L, null);
        String text = PlainTextComponentSerializer.plainText().serialize(card);
        assertTrue(text.contains("Inspección de Cultivo"));
        assertTrue(text.contains("Regado hoy 💧"));
        assertTrue(text.contains("Ninguno"));
        assertFalse(text.contains("<green>"), "Should not leak <green> tag");
        assertFalse(text.contains("<blue>"), "Should not leak <blue> tag");
        assertFalse(text.contains("<dark_gray>"), "Should not leak <dark_gray> tag");
        assertFalse(text.contains("<color>"), "Should not leak <color> tag");
    }

    @Test
    void testFormatCropCardWithMiniMessageMarkupInSpecies() {
        Species tomatoSpecies = new Species() {
            private final Key key = Key.croppy("tomato");
            private final List<StageDefinition> stages = List.of(
                    new StageDefinition("seed", 50.0, 0),
                    new StageDefinition("mature", 0.0, 1)
            );
            private final CropRequirements requirements = new CropRequirements(9, 1);

            @Override public Key getKey() { return key; }
            @Override public String getName() { return "<red>Tomato</red>"; }
            @Override public CropDisplay getDisplay() { return new CropDisplay("3d", "croppy:tomato", "croppy:tomato_seeds"); }
            @Override public List<StageDefinition> getStages() { return stages; }
            @Override public CropRequirements getRequirements() { return requirements; }
            @Override public HarvestDefinition getHarvest() { return null; }
        };

        dev.darkblade.croppy.core.i18n.DefaultMessageCatalog catalog = new dev.darkblade.croppy.core.i18n.DefaultMessageCatalog(
                dev.darkblade.croppy.api.i18n.LocaleRef.EN_US, dev.darkblade.croppy.api.i18n.LocaleRef.EN_US
        );
        dev.darkblade.croppy.core.i18n.YamlLocaleLoader loader = new dev.darkblade.croppy.core.i18n.YamlLocaleLoader();
        try (var enIn = getClass().getResourceAsStream("/locales/en-US.yml")) {
            assertNotNull(enIn);
            catalog.registerLocale(dev.darkblade.croppy.api.i18n.LocaleRef.EN_US, loader.load(enIn, "en-US.yml"));
        } catch (Exception e) {
            fail(e);
        }

        dev.darkblade.croppy.platform.paper.i18n.PaperMessageService messageService =
                new dev.darkblade.croppy.platform.paper.i18n.PaperMessageService(catalog, null);

        CropInspectorService localizedInspector = new CropInspectorService(
                cropManager, speciesRegistry, pos -> CropEnvironment.of(15, 8), null, null, messageService
        );

        BlockPosition pos = new BlockPosition("world", 100, 64, 100);
        DefaultCropInstance crop = new DefaultCropInstance(tomatoSpecies.getKey(), pos, 1, 0.0, 500L, 500L);

        Component card = localizedInspector.formatCropCard(crop, tomatoSpecies, CropEnvironment.of(15, 8), 500L, null);
        String text = PlainTextComponentSerializer.plainText().serialize(card);

        // Verification of bugfix: tags should never leak into plain text output
        assertTrue(text.contains("Tomato"));
        assertFalse(text.contains("<red>"), "Should not leak <red> tag in species name");
        assertFalse(text.contains("</red>"), "Should not leak </red> tag in species name");
        assertFalse(text.contains("<green>"), "Should not leak <green> tag in progress bar or light");
        assertFalse(text.contains("<blue>"), "Should not leak <blue> tag in water");
        assertFalse(text.contains("<dark_gray>"), "Should not leak <dark_gray> tag in progress bar");
        assertFalse(text.contains("<color>"), "Should not leak <color> placeholder tag");
        assertTrue(text.contains("15 (Min: 9)"));
        assertTrue(text.contains("8/8 (Min: 1)"));
        assertTrue(text.contains("■■■■■■■■■■"));
    }

    @Test
    void testInspectMultiBlockCropAtSecondaryCoordinate() {
        BlockPosition rootPos = new BlockPosition("world", 10, 64, 10);
        BlockPosition secondaryPos = new BlockPosition("world", 10, 65, 10);
        DefaultCropInstance crop = new DefaultCropInstance(testSpecies.getKey(), rootPos, 0, 50.0, 100L, 100L);
        cropManager.registerCrop(crop, 2);

        Optional<Component> cardOpt = inspectorService.inspect(secondaryPos);
        assertTrue(cardOpt.isPresent(), "Inspection on secondary Y+1 coordinate must return the crop card");

        String text = PlainTextComponentSerializer.plainText().serialize(cardOpt.get());
        assertTrue(text.contains("Golden Wheat"));
    }
}
