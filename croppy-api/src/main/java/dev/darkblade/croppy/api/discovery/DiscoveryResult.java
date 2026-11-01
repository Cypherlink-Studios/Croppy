package dev.darkblade.croppy.api.discovery;

import java.util.Objects;

/**
 * Result outcome of a recipe discovery evaluation.
 *
 * @param status  result status
 * @param message optional informational message or hint
 */
public record DiscoveryResult(
        Status status,
        String message
) {

    public enum Status {
        /**
         * The recipe was successfully discovered and unlocked.
         */
        DISCOVERED,

        /**
         * A hint or clue was unlocked for the recipe.
         */
        HINTED,

        /**
         * The discovery conditions were not met.
         */
        INELIGIBLE,

        /**
         * The recipe is already known by the player.
         */
        ALREADY_KNOWN
    }

    public static final DiscoveryResult DISCOVERED = new DiscoveryResult(Status.DISCOVERED, null);
    public static final DiscoveryResult INELIGIBLE = new DiscoveryResult(Status.INELIGIBLE, null);
    public static final DiscoveryResult ALREADY_KNOWN = new DiscoveryResult(Status.ALREADY_KNOWN, null);

    public DiscoveryResult {
        Objects.requireNonNull(status, "status cannot be null");
    }

    public static DiscoveryResult hinted(String message) {
        return new DiscoveryResult(Status.HINTED, message);
    }

    public boolean isDiscovered() {
        return status == Status.DISCOVERED;
    }

    public boolean isHinted() {
        return status == Status.HINTED;
    }
}
