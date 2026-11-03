package dev.darkblade.croppy.core.species;

import dev.darkblade.croppy.api.species.RotationMode;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure math utility calculating orientation angles and Y-axis rotation quaternions for 3D displays.
 */
public final class RotationCalculator {

    private RotationCalculator() {}

    /**
     * Normalizes an angle into the standard range [0.0, 360.0).
     *
     * @param yaw input yaw in degrees
     * @return normalized yaw in degrees
     */
    public static float normalize(float yaw) {
        float normalized = yaw % 360.0f;
        if (normalized < 0.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }

    /**
     * Calculates the effective placement yaw based on the configured rotation mode.
     *
     * @param yaw  raw player yaw in degrees
     * @param mode rotation mode
     * @return snapped or adjusted yaw in degrees [0.0, 360.0)
     */
    public static float calculate(float yaw, RotationMode mode) {
        Objects.requireNonNull(mode, "mode must not be null");
        float normalized = normalize(yaw);

        return switch (mode) {
            case CARDINAL_4 -> snapCardinal4(normalized);
            case CARDINAL_8 -> snapCardinal8(normalized);
            case FREE_YAW -> normalized;
            case RANDOM_JITTER -> {
                float jitter = (float) (ThreadLocalRandom.current().nextDouble() * 30.0 - 15.0);
                yield normalize(normalized + jitter);
            }
        };
    }

    /**
     * Deterministic variant of calculate for testing and fixed jitter evaluation.
     *
     * @param yaw          raw player yaw in degrees
     * @param mode         rotation mode
     * @param jitterOffset offset in degrees for RANDOM_JITTER
     * @return adjusted yaw in degrees
     */
    public static float calculate(float yaw, RotationMode mode, float jitterOffset) {
        Objects.requireNonNull(mode, "mode must not be null");
        float normalized = normalize(yaw);

        return switch (mode) {
            case CARDINAL_4 -> snapCardinal4(normalized);
            case CARDINAL_8 -> snapCardinal8(normalized);
            case FREE_YAW -> normalized;
            case RANDOM_JITTER -> normalize(normalized + jitterOffset);
        };
    }

    /**
     * Snaps an angle to the closest 4-way cardinal direction (0°, 90°, 180°, 270°).
     */
    public static float snapCardinal4(float yaw) {
        float normalized = normalize(yaw);
        int sector = Math.round(normalized / 90.0f);
        return (sector * 90.0f) % 360.0f;
    }

    /**
     * Snaps an angle to the closest 8-way compass direction (every 45°).
     */
    public static float snapCardinal8(float yaw) {
        float normalized = normalize(yaw);
        int sector = Math.round(normalized / 45.0f);
        return (sector * 45.0f) % 360.0f;
    }

    /**
     * Computes the Y-axis rotation quaternion [x, y, z, w] for an angle in degrees.
     *
     * @param yawDegrees yaw in degrees
     * @return float array with [x, y, z, w] components
     */
    public static float[] toYQuaternion(float yawDegrees) {
        double radians = Math.toRadians(-yawDegrees);
        float y = (float) Math.sin(radians / 2.0);
        float w = (float) Math.cos(radians / 2.0);
        return new float[]{0.0f, y, 0.0f, w};
    }
}
