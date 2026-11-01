package dev.darkblade.croppy.api.station.visual;

/**
 * Immutable spatial transformation parameters for a display entity.
 *
 * @param translationX spatial X offset relative to station block center
 * @param translationY spatial Y offset relative to station block center
 * @param translationZ spatial Z offset relative to station block center
 * @param pitch        rotation pitch in radians
 * @param yaw          rotation yaw in radians
 * @param roll         rotation roll in radians
 * @param scaleX       scale factor along X axis
 * @param scaleY       scale factor along Y axis
 * @param scaleZ       scale factor along Z axis
 */
public record DisplayTransform(
        double translationX,
        double translationY,
        double translationZ,
        float pitch,
        float yaw,
        float roll,
        float scaleX,
        float scaleY,
        float scaleZ
) {

    public static final DisplayTransform DEFAULT = new DisplayTransform(0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);

    public static DisplayTransform of(double tx, double ty, double tz, float sx, float sy, float sz) {
        return new DisplayTransform(tx, ty, tz, 0.0f, 0.0f, 0.0f, sx, sy, sz);
    }
}
