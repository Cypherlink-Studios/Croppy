package dev.darkblade.croppy.api.process;

import java.util.Objects;

/**
 * Immutable sound effect specification for a process phase.
 *
 * @param sound         sound key or name (e.g. "BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE", "BLOCK_FIRE_EXTINGUISH")
 * @param intervalTicks tick interval between sound playbacks (e.g. 40 = every 2 seconds)
 * @param volume        sound volume (1.0 default)
 * @param pitch         sound pitch (1.0 default)
 */
public record PhaseSound(
        String sound,
        int intervalTicks,
        float volume,
        float pitch
) {
    public PhaseSound {
        Objects.requireNonNull(sound, "sound cannot be null");
        if (intervalTicks <= 0) {
            intervalTicks = 20;
        }
    }

    public static PhaseSound of(String sound, int intervalTicks) {
        return new PhaseSound(sound, intervalTicks, 1.0f, 1.0f);
    }
}
