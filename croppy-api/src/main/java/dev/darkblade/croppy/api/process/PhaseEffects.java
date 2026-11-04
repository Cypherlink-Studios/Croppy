package dev.darkblade.croppy.api.process;

/**
 * Immutable container for phase-level sensory feedback effects.
 *
 * @param particle optional particle effect to emit during phase ticking
 * @param sound    optional periodic sound to play during phase ticking
 */
public record PhaseEffects(
        PhaseParticle particle,
        PhaseSound sound
) {
    public static final PhaseEffects EMPTY = new PhaseEffects(null, null);

    public boolean hasParticle() {
        return particle != null;
    }

    public boolean hasSound() {
        return sound != null;
    }
}
