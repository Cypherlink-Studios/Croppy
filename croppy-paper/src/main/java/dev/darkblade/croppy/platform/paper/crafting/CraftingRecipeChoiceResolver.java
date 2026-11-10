package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Service responsible for converting domain {@link IngredientChoice} declarations
 * into Bukkit {@link RecipeChoice} instances.
 */
public class CraftingRecipeChoiceResolver {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    public CraftingRecipeChoiceResolver(ItemFactory itemFactory) {
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.itemRegistry = itemFactory.getItemRegistry();
    }

    /**
     * Resolves an {@link IngredientChoice} into an appropriate Bukkit {@link RecipeChoice}.
     *
     * @param choice the domain ingredient declaration
     * @return the resolved RecipeChoice
     * @throws IllegalArgumentException if the choice cannot be resolved
     */
    public RecipeChoice resolveChoice(IngredientChoice choice) {
        Objects.requireNonNull(choice, "choice cannot be null");

        // 1. Tag Choice (#minecraft:planks)
        if (choice.isTag()) {
            return resolveTagChoice(choice.normalizedIdentifier());
        }

        String identifier = choice.identifier().trim();

        // 2. Custom Item Choice (croppy:* or registered custom items)
        Key key = null;
        try {
            key = Key.parse(identifier);
        } catch (IllegalArgumentException ignored) {
            // Not a valid key format, fall through to vanilla material matching
        }

        if (key != null) {
            Optional<ItemDefinition> defOpt = itemRegistry.get(key);
            if (defOpt.isPresent()) {
                ItemStack itemStack = itemFactory.createItemStack(defOpt.get(), 1);
                return new RecipeChoice.ExactChoice(itemStack);
            }
        }

        // 3. Vanilla Material Choice
        Material material = matchVanillaMaterial(identifier);
        if (material != null && !isAir(material)) {
            return new RecipeChoice.MaterialChoice(material);
        }

        throw new IllegalArgumentException("Unable to resolve ingredient choice: " + choice.identifier());
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private RecipeChoice resolveTagChoice(String rawTag) {
        NamespacedKey tagKey;
        if (rawTag.contains(":")) {
            String[] parts = rawTag.split(":", 2);
            tagKey = new NamespacedKey(parts[0], parts[1]);
        } else {
            tagKey = NamespacedKey.minecraft(rawTag);
        }

        if (Bukkit.getServer() != null) {
            try {
                Tag<Material> itemTag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
                if (itemTag != null) {
                    return new RecipeChoice.MaterialChoice(itemTag);
                }

                Tag<Material> blockTag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class);
                if (blockTag != null) {
                    return new RecipeChoice.MaterialChoice(blockTag);
                }
            } catch (Throwable ignored) {
            }
        }

        // 2. Fallback for well-known tags in offline/unit-test environments
        List<Material> fallback = resolveFallbackTagMaterials(tagKey.getKey());
        if (!fallback.isEmpty()) {
            return new RecipeChoice.MaterialChoice(fallback);
        }

        throw new IllegalArgumentException("Vanilla tag not found for: #" + rawTag);
    }

    private List<Material> resolveFallbackTagMaterials(String key) {
        if ("planks".equalsIgnoreCase(key)) {
            java.util.List<Material> list = new java.util.ArrayList<>();
            for (Material mat : Material.values()) {
                if (mat.name().endsWith("_PLANKS")) {
                    list.add(mat);
                }
            }
            return list;
        }
        if ("wool".equalsIgnoreCase(key)) {
            java.util.List<Material> list = new java.util.ArrayList<>();
            for (Material mat : Material.values()) {
                if (mat.name().endsWith("_WOOL")) {
                    list.add(mat);
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    private Material matchVanillaMaterial(String raw) {
        String normalized = raw.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        return Material.matchMaterial(normalized);
    }
}
