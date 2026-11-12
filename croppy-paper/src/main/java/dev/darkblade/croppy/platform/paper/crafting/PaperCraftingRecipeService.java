package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.crafting.CraftingType;
import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service managing the registration, unregistration, and Bukkit lifecycle
 * of custom crafting table recipes.
 */
public class PaperCraftingRecipeService {

    private final Server server;
    private final CraftingRecipeRegistry craftingRecipeRegistry;
    private final ItemFactory itemFactory;
    private final CraftingRecipeChoiceResolver choiceResolver;
    private final CroppyLogger logger;
    private final Set<NamespacedKey> registeredKeys = ConcurrentHashMap.newKeySet();

    public PaperCraftingRecipeService(
            Server server,
            CraftingRecipeRegistry craftingRecipeRegistry,
            ItemFactory itemFactory,
            CraftingRecipeChoiceResolver choiceResolver,
            Logger logger
    ) {
        this(
                server,
                craftingRecipeRegistry,
                itemFactory,
                choiceResolver,
                logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE
        );
    }

    public PaperCraftingRecipeService(
            Server server,
            CraftingRecipeRegistry craftingRecipeRegistry,
            ItemFactory itemFactory,
            CraftingRecipeChoiceResolver choiceResolver,
            CroppyLogger logger
    ) {
        this.server = server;
        this.craftingRecipeRegistry = Objects.requireNonNull(craftingRecipeRegistry, "craftingRecipeRegistry cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.choiceResolver = Objects.requireNonNull(choiceResolver, "choiceResolver cannot be null");
        this.logger = (logger != null) ? logger : NoOpCroppyLogger.INSTANCE;
    }

    /**
     * Registers all definitions from the {@link CraftingRecipeRegistry} with Bukkit.
     *
     * @return the number of recipes successfully registered
     */
    public synchronized int registerAll() {
        int registeredCount = 0;
        for (CraftingRecipe recipe : craftingRecipeRegistry.values()) {
            try {
                if (registerRecipe(recipe)) {
                    registeredCount++;
                }
            } catch (Exception e) {
                logger.warn(LogChannel.LOADER, "Failed to register crafting recipe '" + recipe.getKey() + "': " + e.getMessage(), e);
            }
        }
        int finalCount = registeredCount;
        logger.debug(LogChannel.LOADER, () -> "Registered " + finalCount + " custom crafting recipes into Bukkit.");
        return registeredCount;
    }

    /**
     * Translates and registers a single domain {@link CraftingRecipe} into Bukkit.
     *
     * @param recipe the recipe to register
     * @return true if successfully registered, false otherwise
     */
    public synchronized boolean registerRecipe(CraftingRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe cannot be null");
        NamespacedKey recipeKey = toNamespacedKey(recipe.getKey());

        // Remove if previously registered to avoid duplicates on reload
        removeBukkitRecipe(recipeKey);

        ItemStack result = createResultStack(recipe.resultItemKey(), recipe.resultAmount());
        if (result == null || isAir(result.getType())) {
            logger.warn(LogChannel.LOADER, "Skipping recipe '" + recipe.getKey() + "': invalid result item '" + recipe.resultItemKey() + "'");
            return false;
        }

        if (recipe.type() == CraftingType.SHAPED) {
            ShapedRecipe shaped = new ShapedRecipe(recipeKey, result);
            shaped.shape(recipe.shape().toArray(new String[0]));

            for (Map.Entry<Character, IngredientChoice> entry : recipe.shapedIngredients().entrySet()) {
                RecipeChoice choice = choiceResolver.resolveChoice(entry.getValue());
                shaped.setIngredient(entry.getKey(), choice);
            }

            if (recipe.group() != null && !recipe.group().isBlank()) {
                shaped.setGroup(recipe.group());
            }
            shaped.setCategory(parseCraftingCategory(recipe.category()));

            addBukkitRecipe(shaped);
            registeredKeys.add(recipeKey);
            return true;
        } else if (recipe.type() == CraftingType.SHAPELESS) {
            ShapelessRecipe shapeless = new ShapelessRecipe(recipeKey, result);

            for (IngredientChoice ingredient : recipe.shapelessIngredients()) {
                RecipeChoice choice = choiceResolver.resolveChoice(ingredient);
                for (int i = 0; i < ingredient.amount(); i++) {
                    shapeless.addIngredient(choice);
                }
            }

            if (recipe.group() != null && !recipe.group().isBlank()) {
                shapeless.setGroup(recipe.group());
            }
            shapeless.setCategory(parseCraftingCategory(recipe.category()));

            addBukkitRecipe(shapeless);
            registeredKeys.add(recipeKey);
            return true;
        }

        return false;
    }

    /**
     * Unregisters all tracked recipes from Bukkit.
     */
    public synchronized void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            removeBukkitRecipe(key);
        }
        int count = registeredKeys.size();
        registeredKeys.clear();
        logger.debug(LogChannel.LOADER, () -> "Unregistered " + count + " custom crafting recipes from Bukkit.");
    }

    /**
     * Unregisters a specific recipe key from Bukkit.
     *
     * @param key the recipe key to unregister
     * @return true if previously registered and now removed
     */
    public synchronized boolean unregisterRecipe(NamespacedKey key) {
        if (key != null && registeredKeys.remove(key)) {
            removeBukkitRecipe(key);
            return true;
        }
        return false;
    }

    /**
     * @return an unmodifiable view of all registered Bukkit recipe keys
     */
    public Set<NamespacedKey> getRegisteredKeys() {
        return Collections.unmodifiableSet(registeredKeys);
    }

    private ItemStack createResultStack(Key itemKey, int amount) {
        return itemFactory.resolveItemStack(itemKey.asString(), amount).orElse(null);
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private NamespacedKey toNamespacedKey(Key key) {
        return new NamespacedKey(key.namespace(), key.value());
    }

    private CraftingBookCategory parseCraftingCategory(String category) {
        if (category == null || category.isBlank()) {
            return CraftingBookCategory.MISC;
        }
        try {
            return CraftingBookCategory.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return CraftingBookCategory.MISC;
        }
    }

    private void addBukkitRecipe(org.bukkit.inventory.Recipe recipe) {
        if (server != null) {
            server.addRecipe(recipe);
        } else {
            Bukkit.addRecipe(recipe);
        }
    }

    private void removeBukkitRecipe(NamespacedKey key) {
        try {
            if (server != null) {
                server.removeRecipe(key);
            } else {
                Bukkit.removeRecipe(key);
            }
        } catch (Exception ignored) {
        }
    }
}
