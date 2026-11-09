package dev.darkblade.croppy.api.update;

import java.util.Locale;

/**
 * Update distribution channels supported by Croppy.
 */
public enum UpdateChannel {
    /**
     * Official stable production releases only (excludes pre-releases and draft tags).
     */
    STABLE,

    /**
     * Bleeding-edge pre-releases (including -alpha, -beta, -rc builds).
     */
    BETA;

    /**
     * Parses an update channel from a string safely, defaulting to {@link #STABLE}.
     *
     * @param rawName the raw channel name
     * @return the resolved UpdateChannel
     */
    public static UpdateChannel fromString(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return STABLE;
        }
        try {
            return UpdateChannel.valueOf(rawName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return STABLE;
        }
    }
}
