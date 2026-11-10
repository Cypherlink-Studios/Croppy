package dev.darkblade.croppy.api.crafting;

import java.util.Objects;

/**
 * Immutable declaration of an ingredient requirement in a crafting recipe.
 *
 * @param identifier the item key (e.g. "minecraft:iron_ingot", "croppy:wooden_can") or tag (e.g. "#minecraft:planks")
 * @param amount     the quantity required (typically 1 for shaped slot, &gt;= 1 for shapeless)
 */
public record IngredientChoice(String identifier, int amount) {

    public IngredientChoice {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        if (identifier.isBlank()) {
            throw new IllegalArgumentException("identifier cannot be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, got: " + amount);
        }
    }

    public IngredientChoice(String identifier) {
        this(identifier, 1);
    }

    /**
     * Checks whether this choice specifies a vanilla tag reference (prefixed with '#').
     *
     * @return true if this is a tag reference
     */
    public boolean isTag() {
        return identifier.startsWith("#");
    }

    /**
     * Gets the normalized identifier without the leading '#' if present.
     *
     * @return normalized identifier
     */
    public String normalizedIdentifier() {
        return isTag() ? identifier.substring(1) : identifier;
    }
}
