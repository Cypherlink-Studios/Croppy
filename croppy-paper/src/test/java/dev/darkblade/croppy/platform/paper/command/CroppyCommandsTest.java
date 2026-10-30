package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CroppyCommandsTest {

    private DefaultItemRegistry itemRegistry;
    private CroppyCommands commands;

    private Plugin createMockPlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "croppy";
                    return null;
                }
        );
    }

    @BeforeEach
    void setUp() {
        itemRegistry = new DefaultItemRegistry();
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        commands = new CroppyCommands(
                (dev.darkblade.croppy.platform.paper.CroppyPlugin) null,
                inspector,
                new PluginConfig(),
                null,
                null,
                null,
                null,
                itemRegistry
        ) {
            // override or sub-class if needed
        };
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
}
