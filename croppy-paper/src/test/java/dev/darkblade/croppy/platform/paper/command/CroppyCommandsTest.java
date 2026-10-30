package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import org.incendo.cloud.context.CommandInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "rusty_can"), "watering_cans", "IRON_INGOT", 0, "Rusty Can", List.of(), Map.of()));
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "wheat_seeds"), "seeds", "WHEAT_SEEDS", 0, "Seeds", List.of(), Map.of()));

        List<String> allItems = commands.suggestCategoryItems(null, CommandInput.empty());
        assertEquals(2, allItems.size());
        assertTrue(allItems.contains("croppy:rusty_can"));
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
}
