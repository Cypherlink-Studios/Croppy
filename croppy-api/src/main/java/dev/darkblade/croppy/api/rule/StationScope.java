package dev.darkblade.croppy.api.rule;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Objects;
import java.util.Set;

/**
 * Filter scoping rule execution to specific crafting station identifiers.
 *
 * @param mode        filtering mode (INCLUDE or EXCLUDE)
 * @param stationKeys set of station keys targeted by the filter
 */
public record StationScope(Mode mode, Set<Key> stationKeys) {

    public enum Mode {
        INCLUDE,
        EXCLUDE
    }

    public static final StationScope ALL = new StationScope(Mode.EXCLUDE, Set.of());

    public StationScope {
        mode = (mode == null) ? Mode.INCLUDE : mode;
        stationKeys = (stationKeys == null) ? Set.of() : Set.copyOf(stationKeys);
    }

    public static StationScope include(Key... keys) {
        return new StationScope(Mode.INCLUDE, Set.of(keys));
    }

    public static StationScope include(Set<Key> keys) {
        return new StationScope(Mode.INCLUDE, keys);
    }

    public static StationScope exclude(Key... keys) {
        return new StationScope(Mode.EXCLUDE, Set.of(keys));
    }

    public static StationScope exclude(Set<Key> keys) {
        return new StationScope(Mode.EXCLUDE, keys);
    }

    public static StationScope all() {
        return ALL;
    }

    /**
     * Tests whether a station key matches this scope.
     *
     * @param stationKey station identifier to check
     * @return true if permitted by this scope
     */
    public boolean matches(Key stationKey) {
        if (stationKey == null) {
            return false;
        }
        boolean contains = stationKeys.contains(stationKey);
        return (mode == Mode.INCLUDE) ? contains : !contains;
    }
}
