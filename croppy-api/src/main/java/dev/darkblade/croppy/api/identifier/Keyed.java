package dev.darkblade.croppy.api.identifier;

/**
 * Represents any domain object identifiable by an immutable {@link Key}.
 */
public interface Keyed {
    /**
     * @return the unique key identifying this object
     */
    Key getKey();
}
