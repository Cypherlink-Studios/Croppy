package dev.darkblade.croppy.api.species;

/**
 * Environmental requirements for a crop species.
 *
 * @param minLight minimum light level required for growth (0 to 15)
 * @param waterFluidLevel minimum water fluid level required (1 to 8, or 0 if dry-farming)
 */
public record CropRequirements(int minLight, int waterFluidLevel) {
    public CropRequirements {
        if (minLight < 0 || minLight > 15) {
            throw new IllegalArgumentException("minLight must be between 0 and 15, was " + minLight);
        }
        if (waterFluidLevel < 0 || waterFluidLevel > 8) {
            throw new IllegalArgumentException("waterFluidLevel must be between 0 and 8, was " + waterFluidLevel);
        }
    }

    /**
     * @return true if this crop requires nearby water to grow
     */
    public boolean requiresWater() {
        return waterFluidLevel > 0;
    }
}
