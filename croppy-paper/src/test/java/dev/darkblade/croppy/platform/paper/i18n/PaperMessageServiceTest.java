package dev.darkblade.croppy.platform.paper.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.core.i18n.DefaultMessageCatalog;
import dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperMessageServiceTest {

    private DefaultMessageCatalog catalog;
    private PaperMessageService messageService;

    @BeforeEach
    void setUp() {
        catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);

        Map<String, String> enUS = Map.of(
                "general.prefix", "<gold>[Croppy]</gold> ",
                "command.give.success", "<green>Given <yellow><amount>x</yellow> <item> to <player>.</green>",
                "test.missing_key", "Only in EN",
                "content.item.croppy.wheat_seeds.name", "<yellow>Golden Wheat Seeds</yellow>"
        );

        Map<String, String> esES = Map.of(
                "general.prefix", "<gold>[Croppy]</gold> ",
                "command.give.success", "<green>Entregado <yellow><amount>x</yellow> <item> a <player>.</green>",
                "content.item.croppy.wheat_seeds.name", "<yellow>Semillas de Trigo Doradas</yellow>"
        );

        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, enUS,
                LocaleRef.ES_ES, esES
        ));

        messageService = new PaperMessageService(catalog, dev.darkblade.croppy.api.logging.NoOpCroppyLogger.INSTANCE);
    }

    @Test
    void testResolveCategoryDisplayNameParsesMiniMessageAndPreservesTags() {
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();

        Component byKey = messageService.resolveCategoryDisplayName(Audience.empty(), Key.croppy("soups"), categoryRegistry);
        Component byRecord = messageService.resolveCategoryDisplayName(Audience.empty(), RecipeCategory.SOUPS);

        assertEquals("Soups & Stews", PlainTextComponentSerializer.plainText().serialize(byKey));
        assertEquals("Soups & Stews", PlainTextComponentSerializer.plainText().serialize(byRecord));

        String mini = MiniMessage.miniMessage().serialize(byKey);
        assertFalse(mini.contains("\\<aqua>"), "Should parse MiniMessage tags instead of escaping them: " + mini);
        assertTrue(mini.contains("<aqua>Soups & Stews"), "Should preserve aqua styling, got: " + mini);

        assertEquals(Component.empty(), messageService.resolveCategoryDisplayName(Audience.empty(), null));
        assertEquals(Component.empty(), messageService.resolveCategoryDisplayName(Audience.empty(), null, categoryRegistry));
    }

    @Test
    void testResolveCategoryDisplayNameUsesLocaleOverride() {
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "content.category.croppy.soups.name", "<gold><bold>Hot Soups</bold></gold>"
                )
        ));

        Component byKey = messageService.resolveCategoryDisplayName(Audience.empty(), Key.croppy("soups"), categoryRegistry);
        Component byRecord = messageService.resolveCategoryDisplayName(Audience.empty(), RecipeCategory.SOUPS);

        assertEquals("Hot Soups", PlainTextComponentSerializer.plainText().serialize(byKey));
        assertEquals("Hot Soups", PlainTextComponentSerializer.plainText().serialize(byRecord));

        String mini = MiniMessage.miniMessage().serialize(byKey);
        assertTrue(mini.contains("<gold>") && mini.contains("<bold>") && mini.contains("Hot Soups"),
                "Override styling should be preserved: " + mini);
    }

    @Test
    void testResolveCategoryDisplayNameFallsBackForUnregisteredKeys() {
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();
        Key customKey = Key.of("custom", "street_food");

        Component unregistered = messageService.resolveCategoryDisplayName(Audience.empty(), customKey, categoryRegistry);
        Component nullRegistry = messageService.resolveCategoryDisplayName(Audience.empty(), Key.croppy("soups"), null);

        assertEquals("street_food", PlainTextComponentSerializer.plainText().serialize(unregistered));
        assertEquals("soups", PlainTextComponentSerializer.plainText().serialize(nullRegistry));

        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "content.category.custom.street_food.name", "<green>Street Food</green>"
                )
        ));
        Component overriddenUnregistered = messageService.resolveCategoryDisplayName(Audience.empty(), customKey, categoryRegistry);
        assertEquals("Street Food", PlainTextComponentSerializer.plainText().serialize(overriddenUnregistered));
        assertTrue(MiniMessage.miniMessage().serialize(overriddenUnregistered).contains("<green>Street Food"));
    }

    @Test
    void testRenderWithLocaleAndPlaceholders() {
        Component comp = messageService.render(
                LocaleRef.ES_ES,
                MessageKey.of("command.give.success"),
                PaperMessageService.args("amount", 3, "item", "Trigo", "player", "Steve")
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        assertEquals("Entregado 3x Trigo a Steve.", plain);
    }

    @Test
    void testFallbackToEnglish() {
        Component comp = messageService.render(
                LocaleRef.ES_ES,
                MessageKey.of("test.missing_key")
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        assertEquals("Only in EN", plain);
    }

    @Test
    void testMissingKeyToken() {
        Component comp = messageService.render(
                LocaleRef.ES_ES,
                MessageKey.of("unknown.key")
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        assertEquals("<missing:croppy:unknown.key>", plain);
    }

    @Test
    void testArgumentSanitizationPreventsInjection() {
        // Player name contains MiniMessage exploit tags
        String maliciousName = "<red><click:run_command:/op>Steve</click></red>";

        Component comp = messageService.render(
                LocaleRef.EN_US,
                MessageKey.of("command.give.success"),
                PaperMessageService.args("amount", 1, "item", "Wheat", "player", maliciousName)
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        // The raw tag text should be preserved literally, not executed/stripped
        assertTrue(plain.contains("<red><click:run_command:/op>Steve</click></red>"));
    }

    @Test
    void testContentLocalizationOverride() {
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "wheat_seeds"),
                "seeds",
                "minecraft:wheat_seeds",
                0,
                "<green>Default Wheat Seeds</green>",
                List.of("<gray>Plant me</gray>"),
                Map.of()
        );

        // In EN, override exists ("Golden Wheat Seeds")
        Component nameEN = messageService.resolveItemDisplayName(null, def);
        assertEquals("Golden Wheat Seeds", PlainTextComponentSerializer.plainText().serialize(nameEN));

        // For item without override, uses definition display name
        ItemDefinition carrot = new ItemDefinition(
                Key.of("croppy", "carrot_seeds"),
                "seeds",
                "minecraft:carrot",
                0,
                "<green>Default Carrot Seeds</green>",
                List.of(),
                Map.of()
        );

        Component carrotName = messageService.resolveItemDisplayName(null, carrot);
        assertEquals("Default Carrot Seeds", PlainTextComponentSerializer.plainText().serialize(carrotName));
    }

    @Test
    void testStationTemperatureTemplate() {
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "station.temperature", "<gradient:#ffaa00:{color}>Temperature:</gradient> <white><temperature>°C</white> <gray>[<zone>]</gray>",
                        "station.empty-feedback", "<name> <dark_gray>•</dark_gray> <status> <dark_gray>•</dark_gray> <gradient:#ffaa00:{color}><temperature>°C</gradient>"
                )
        ));
        assertDoesNotThrow(() -> {
            Component c = messageService.renderWithReplacements(
                    LocaleRef.EN_US,
                    MessageKey.of("station.temperature"),
                    Map.of("{color}", "#55ffff"),
                    PaperMessageService.args("temperature", "25.0", "zone", "WARM")
            );
            assertEquals("Temperature: 25.0°C [WARM]", PlainTextComponentSerializer.plainText().serialize(c));

            Component fb = messageService.renderWithReplacements(
                    LocaleRef.EN_US,
                    MessageKey.of("station.empty-feedback"),
                    Map.of("{color}", "#55ffff"),
                    PaperMessageService.args("name", "Cooking Pot", "status", "Empty", "temperature", "25.0")
            );
            assertEquals("Cooking Pot • Empty • 25.0°C", PlainTextComponentSerializer.plainText().serialize(fb));
        });
    }

    @Test
    void testTagStylingForColorTag() {
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "inspector.light", "<dark_gray>├─ <gold>Light Level: <color><level></color> <gray>(Min: <min>)</gray>"
                )
        ));
        Component c = messageService.render(
                LocaleRef.EN_US,
                MessageKey.of("inspector.light"),
                PaperMessageService.args(
                        "color", net.kyori.adventure.text.format.NamedTextColor.GREEN,
                        "level", 15,
                        "min", 9
                )
        );
        String plain = PlainTextComponentSerializer.plainText().serialize(c);
        assertEquals("├─ Light Level: 15 (Min: 9)", plain);
        String mini = MiniMessage.miniMessage().serialize(c);
        assertTrue(mini.contains("<green>15"), "Should contain green 15, but was: " + mini);
    }

    @Test
    void testResolveSpeciesDisplayName() {
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "content.species.croppy.wheat.name", "<yellow>Golden Wheat</yellow>"
                ),
                LocaleRef.ES_ES, Map.of(
                        "content.species.croppy.wheat.name", "<yellow>Trigo Dorado</yellow>"
                )
        ));

        dev.darkblade.croppy.api.species.Species wheat = new dev.darkblade.croppy.api.species.Species() {
            @Override public Key getKey() { return Key.croppy("wheat"); }
            @Override public String getName() { return "<gold>Default Wheat</gold>"; }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() { return null; }
            @Override public List<dev.darkblade.croppy.api.species.StageDefinition> getStages() { return List.of(); }
            @Override public dev.darkblade.croppy.api.species.CropRequirements getRequirements() { return null; }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return null; }
        };

        Component enName = messageService.resolveSpeciesDisplayName(null, wheat);
        assertEquals("Golden Wheat", PlainTextComponentSerializer.plainText().serialize(enName));

        // Unknown species falls back to species definition getName()
        dev.darkblade.croppy.api.species.Species tomato = new dev.darkblade.croppy.api.species.Species() {
            @Override public Key getKey() { return Key.croppy("tomato"); }
            @Override public String getName() { return "<red>Tomato</red>"; }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() { return null; }
            @Override public List<dev.darkblade.croppy.api.species.StageDefinition> getStages() { return List.of(); }
            @Override public dev.darkblade.croppy.api.species.CropRequirements getRequirements() { return null; }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return null; }
        };

        Component tomatoName = messageService.resolveSpeciesDisplayName(null, tomato);
        assertEquals("Tomato", PlainTextComponentSerializer.plainText().serialize(tomatoName));
        String mini = MiniMessage.miniMessage().serialize(tomatoName);
        assertEquals("<red>Tomato", mini);
    }

    @Test
    void testItemEntryClickAndHoverRenderingWithoutTagLeakage() {
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "command.list.item-entry", " <dark_gray>•</dark_gray> <click:suggest_command:'/croppy item give {category} {key} 1'><hover:show_text:'<gray>Click to give 1x <white><name></white>'><yellow>{key}</yellow></hover></click> <dark_gray>-</dark_gray> <name>"
                )
        ));

        ItemDefinition burntFood = new ItemDefinition(
                Key.of("croppy", "burnt_food"),
                "misc",
                "minecraft:charcoal",
                0,
                "<dark_gray>Burnt Food</dark_gray>",
                List.of(),
                Map.of()
        );

        Component itemName = messageService.resolveItemDisplayName(null, burntFood);
        Component rendered = messageService.renderWithReplacements(
                LocaleRef.EN_US,
                MessageKey.of("command.list.item-entry"),
                PaperMessageService.rawArgs("category", "misc", "key", "croppy:burnt_food"),
                PaperMessageService.arg("name", itemName)
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        // Must NOT leak raw <click> or <dark_gray> tags into plain text
        assertFalse(plain.contains("<click:"));
        assertFalse(plain.contains("<dark_gray>"));
        assertTrue(plain.contains("croppy:burnt_food"));
        assertTrue(plain.contains("Burnt Food"));

        String serializedMM = MiniMessage.miniMessage().serialize(rendered);
        assertTrue(serializedMM.contains("<click:suggest_command:'/croppy item give misc croppy:burnt_food 1'>"));
    }

    @Test
    void testResolveRecipeDisplayNamePreservesGradients() {
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "compendium.gui.discovered-title", "<gold><bold><recipe></bold></gold>"
                )
        ));

        RecipeDefinition coffeeRecipe = new RecipeDefinition(
                Key.of("coffee", "roasting_medium"),
                "<gradient:#e65c00:#f9d423>Medium City Roast</gradient>",
                Key.of("coffee", "roasting"),
                Key.of("coffee", "coffee_roaster"),
                Key.of("coffee", "medium_city_roast"),
                List.of(),
                Key.of("coffee", "medium_city_roast"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        Component recipeName = messageService.resolveRecipeDisplayName(null, coffeeRecipe);
        assertEquals("Medium City Roast", PlainTextComponentSerializer.plainText().serialize(recipeName));

        Component title = messageService.render(
                LocaleRef.EN_US,
                MessageKey.of("compendium.gui.discovered-title"),
                PaperMessageService.arg("recipe", recipeName)
        );

        String plainTitle = PlainTextComponentSerializer.plainText().serialize(title);
        // Must NOT leak raw gradient tag
        assertFalse(plainTitle.contains("<gradient:"));
        assertEquals("Medium City Roast", plainTitle);

        String mmTitle = MiniMessage.miniMessage().serialize(title);
        assertTrue(mmTitle.toLowerCase().contains("gradient:#e65c00:#f9d423"));
    }
}
