package dev.darkblade.croppy.api.growth;

/**
 * Represents the ambient physical environment of a crop instance at a given moment.
 *
 * @param lightLevel the ambient block light level (0-15)
 * @param waterFluidLevel the highest water fluid level available nearby (0-8, where 8 is source)
 * @param isLoaded whether the crop's chunk/location is loaded and accessible
 */
public record CropEnvironment(int lightLevel, int waterFluidLevel, boolean isLoaded) {

    /**
     * Creates an environment representing an unloaded or inaccessible location.
     */
    public static CropEnvironment unloaded() {
        return new CropEnvironment(0, 0, false);
    }

    /**
     * Creates an active, loaded environment with the specified light and water fluid levels.
     */
    public static CropEnvironment of(int lightLevel, int waterFluidLevel) {
        return new CropEnvironment(lightLevel, waterFluidLevel, true);
    }
}
