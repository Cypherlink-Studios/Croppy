package dev.darkblade.croppy.api.process;

import java.util.Objects;

/**
 * Immutable particle effect specification for a process phase.
 *
 * @param type    particle identifier or name (e.g. "SPLASH", "SMOKE", "FLAME")
 * @param count   number of particles spawned per emit tick
 * @param offsetX randomized X offset spread
 * @param offsetY randomized Y offset spread
 * @param offsetZ randomized Z offset spread
 * @param speed   particle speed or extra data
 */
public record PhaseParticle(
        String type,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double speed
) {
    public PhaseParticle {
        Objects.requireNonNull(type, "type cannot be null");
        if (count <= 0) {
            count = 1;
        }
    }

    public static PhaseParticle of(String type, int count) {
        return new PhaseParticle(type, count, 0.15, 0.05, 0.15, 0.02);
    }
}
