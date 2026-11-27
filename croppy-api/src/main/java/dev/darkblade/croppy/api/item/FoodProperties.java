package dev.darkblade.croppy.api.item;

import java.util.Objects;

/**
 * Immutable food nutrition specification for a consumable {@link ItemDefinition}.
 *
 * @param hunger         hunger points restored (0..20)
 * @param saturation     saturation value restored
 * @param canAlwaysEat   whether the player can consume this item even with a full hunger bar
 * @param qualityScaling optional multipliers applied per item quality level
 */
public record FoodProperties(
        int hunger,
        float saturation,
        boolean canAlwaysEat,
        QualityScaling qualityScaling
) {
    public FoodProperties {
        if (hunger < 0) {
            throw new IllegalArgumentException("hunger cannot be negative: " + hunger);
        }
        if (!Float.isFinite(saturation) || saturation < 0.0f) {
            throw new IllegalArgumentException("saturation must be finite and non-negative: " + saturation);
        }
        qualityScaling = (qualityScaling == null) ? QualityScaling.NONE : qualityScaling;
    }

    public FoodProperties(int hunger, float saturation) {
        this(hunger, saturation, false, QualityScaling.NONE);
    }

    public FoodProperties(int hunger, float saturation, boolean canAlwaysEat) {
        this(hunger, saturation, canAlwaysEat, QualityScaling.NONE);
    }
}
