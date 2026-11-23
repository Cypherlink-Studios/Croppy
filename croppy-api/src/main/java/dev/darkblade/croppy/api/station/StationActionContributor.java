package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Optional;
import java.util.UUID;

/**
 * Pure-domain extension point allowing addons to handle or augment station menu actions
 * (such as {@code EXPERIMENT} or {@code START_PROCESS} fallbacks).
 */
public interface StationActionContributor extends Keyed {

    /**
     * Returns whether this contributor applies to the given station and action name.
     *
     * @param station target station instance
     * @param action  action identifier (e.g. {@code "EXPERIMENT"}, {@code "START_PROCESS"})
     * @return true if this contributor should be consulted
     */
    boolean supports(StationInstance station, String action);

    /**
     * Executes the action for {@code playerId} on {@code station}.
     *
     * @param playerId clicking player UUID
     * @param station  target station instance
     * @param action   action identifier
     * @return empty if not handled (allowing core fallback), or present with optional MiniMessage feedback
     */
    Optional<String> execute(UUID playerId, StationInstance station, String action);
}
