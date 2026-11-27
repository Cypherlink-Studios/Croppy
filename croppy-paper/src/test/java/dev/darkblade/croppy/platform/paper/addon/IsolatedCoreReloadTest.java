package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonState;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemCategory;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IsolatedCoreReloadTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("purgeCoreOwnedRegistries removes core entries while keeping enabled addon entries intact")
    void testPurgeCoreOwnedRegistriesPreservesAddons() throws IOException {
        Path addonsDir = tempDir.resolve("addons");
        Path addonDir = addonsDir.resolve("star-addon");
        Files.createDirectories(addonDir.resolve("crops"));
        Files.createDirectories(addonDir.resolve("items/ingredients"));
        Files.createDirectories(addonDir.resolve("recipes"));
        Files.createDirectories(addonDir.resolve("menus"));
        Files.createDirectories(addonDir.resolve("interactions"));

        Files.writeString(addonDir.resolve("addon.yml"), """
                id: "star-addon"
                name: "Star Addon"
                version: "1.0.0"
                namespace: "star_addon"
                """, StandardCharsets.UTF_8);

        Files.writeString(addonDir.resolve("items/ingredients/star_fruit.yml"), """
                id: "star_addon:star_fruit"
                material: "APPLE"
                name: "Star Fruit"
                """, StandardCharsets.UTF_8);

        Files.writeString(addonDir.resolve("crops/star_berry.yml"), """
                id: "star_addon:star_berry"
                name: "Star Berry"
                display:
                  type: "vanilla"
                  block: "minecraft:sweet_berry_bush"
                  seed_item: "star_addon:star_fruit"
                stages:
                  - id: "seed"
                    progress_required: 100
                requirements:
                  light:
                    min: 9
                """, StandardCharsets.UTF_8);

        Files.writeString(addonDir.resolve("recipes/star_juice.yml"), """
                id: "star_addon:star_juice"
                name: "Star Juice"
                category: "beverages"
                station: "croppy:skillet"
                result:
                  item: "star_addon:star_fruit"
                  amount: 1
                inputs:
                  - item: "star_addon:star_fruit"
                    amount: 2
                """, StandardCharsets.UTF_8);

        CroppyCore core = new CroppyCore();
        core.initialize();

        // Register Croppy core entries
        core.getItemRegistry().register(new ItemDefinition(
                Key.of("croppy", "tomato"), "ingredients", "APPLE", 0, "Tomato", List.of(), Map.of()
        ));
        core.getRecipeRegistry().register(new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "tomato_soup_p"),
                List.of(),
                Key.of("croppy", "tomato"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        ));

        PaperAddonManager addonManager = new PaperAddonManager(null, core, addonsDir, null);
        addonManager.enableAll();
        assertEquals(AddonState.ENABLED, addonManager.getState("star-addon"));

        // Register an addon-owned custom recipe category via context
        PaperAddonContext context = addonManager.getContext("star-addon").orElseThrow();
        Key customCatKey = Key.of("star_addon", "tropical");
        core.getRecipeCategoryRegistry().register(new RecipeCategory(customCatKey, "Tropical", Key.of("star_addon", "star_fruit")));
        context.trackCategory(customCatKey);

        CroppyPlugin.purgeCoreOwnedRegistries(core, addonManager, null, new DefaultFertilizerRegistry());

        // Core items and recipes were purged
        assertFalse(core.getItemRegistry().contains(Key.of("croppy", "tomato")));
        assertFalse(core.getRecipeRegistry().contains(Key.of("croppy", "tomato_soup")));

        // Default categories were restored
        assertTrue(core.getItemCategoryRegistry().contains(ItemCategory.SEEDS.key()));
        assertTrue(core.getRecipeCategoryRegistry().contains(RecipeCategory.SOUPS.getKey()));

        // Addon remained enabled and all addon-owned entries remained intact
        assertEquals(AddonState.ENABLED, addonManager.getState("star-addon"));
        assertTrue(core.getItemRegistry().contains(Key.of("star_addon", "star_fruit")));
        assertTrue(core.getSpeciesRegistry().contains(Key.of("star_addon", "star_berry")));
        assertTrue(core.getRecipeRegistry().contains(Key.of("star_addon", "star_juice")));
        assertTrue(core.getRecipeCategoryRegistry().contains(customCatKey));
    }
}
