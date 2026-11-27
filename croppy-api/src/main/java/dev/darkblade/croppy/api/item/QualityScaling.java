package dev.darkblade.croppy.api.item;

/**
 * Optional per-item quality scaling multipliers applied per quality grade above Normal (0..3).
 *
 * @param hungerPerLevel     fractional hunger bonus per quality level (e.g. 0.25 = +25% per level)
 * @param saturationPerLevel fractional saturation bonus per quality level
 * @param durationPerLevel   fractional effect duration bonus per quality level
 */
public record QualityScaling(
        double hungerPerLevel,
        double saturationPerLevel,
        double durationPerLevel
) {
    public static final QualityScaling NONE = new QualityScaling(0.0, 0.0, 0.0);

    public QualityScaling {
        validateMultiplier("hungerPerLevel", hungerPerLevel);
        validateMultiplier("saturationPerLevel", saturationPerLevel);
        validateMultiplier("durationPerLevel", durationPerLevel);
    }

    private static void validateMultiplier(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }

    /**
     * Scales base hunger according to item quality grade.
     */
    public int scaleHunger(int baseHunger, int quality) {
        if (baseHunger <= 0 || quality <= 0 || hungerPerLevel <= 0.0) {
            return Math.max(0, baseHunger);
        }
        return (int) Math.round(baseHunger * (1.0 + quality * hungerPerLevel));
    }

    /**
     * Scales base saturation according to item quality grade.
     */
    public float scaleSaturation(float baseSaturation, int quality) {
        if (baseSaturation <= 0.0f || quality <= 0 || saturationPerLevel <= 0.0) {
            return Math.max(0.0f, baseSaturation);
        }
        return (float) (baseSaturation * (1.0 + quality * saturationPerLevel));
    }

    /**
     * Scales base duration ticks according to item quality grade.
     */
    public long scaleDurationTicks(long baseTicks, int quality) {
        if (baseTicks <= 0L || quality <= 0 || durationPerLevel <= 0.0) {
            return Math.max(0L, baseTicks);
        }
        return Math.round(baseTicks * (1.0 + quality * durationPerLevel));
    }
}
