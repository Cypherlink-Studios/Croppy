package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeChoiceResolverTest {

    private DefaultItemRegistry itemRegistry;
    private ItemFactory itemFactory;
    private CraftingRecipeChoiceResolver resolver;

    @BeforeEach
    void setUp() {
        itemRegistry = new DefaultItemRegistry();
        itemFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                return new MockTestItemStack(Material.CAULDRON, amount);
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                return new MockTestItemStack(material, amount);
            }
        };
        resolver = new CraftingRecipeChoiceResolver(itemFactory);
    }

    @Test
    @DisplayName("Resolve vanilla material ingredient returns MaterialChoice")
    void resolveVanillaMaterial() {
        IngredientChoice choice = new IngredientChoice("minecraft:iron_ingot");
        RecipeChoice resolved = resolver.resolveChoice(choice);

        assertInstanceOf(RecipeChoice.MaterialChoice.class, resolved);
        RecipeChoice.MaterialChoice matChoice = (RecipeChoice.MaterialChoice) resolved;
        assertTrue(matChoice.getChoices().contains(Material.IRON_INGOT));
    }

    @Test
    @DisplayName("Resolve registered custom item returns ExactChoice")
    void resolveCustomItemExactChoice() {
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "cooking_pot"),
                "stations",
                "minecraft:cauldron",
                101,
                "Cooking Pot",
                List.of(),
                Map.of()
        );
        itemRegistry.register(def);

        IngredientChoice choice = new IngredientChoice("croppy:cooking_pot");
        RecipeChoice resolved = resolver.resolveChoice(choice);

        assertInstanceOf(RecipeChoice.ExactChoice.class, resolved);
        RecipeChoice.ExactChoice exactChoice = (RecipeChoice.ExactChoice) resolved;
        assertEquals(Material.CAULDRON, exactChoice.getItemStack().getType());
    }

    @Test
    @DisplayName("Unresolvable identifier throws IllegalArgumentException")
    void unresolvableIdentifierThrows() {
        IngredientChoice choice = new IngredientChoice("croppy:non_existent_item_xyz");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveChoice(choice));
    }
}
