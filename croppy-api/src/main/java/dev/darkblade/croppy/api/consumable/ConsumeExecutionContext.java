package dev.darkblade.croppy.api.consumable;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.Optional;
import java.util.UUID;

/**
 * Execution context passed to {@link ConsumeEffectHandler}s when a player consumes a custom item.
 */
public interface ConsumeExecutionContext {

    /**
     * @return unique identifier of the consuming player
     */
    UUID getPlayerId();

    /**
     * @return name of the consuming player
     */
    String getPlayerName();

    /**
     * @return item definition of the consumed item
     */
    ItemDefinition getItemDefinition();

    /**
     * @return optional reconstructed stored item carrying quality and metadata properties
     */
    Optional<StoredItem> getStoredItem();

    /**
     * @return effective quality grade in {@code [0, 3]}
     */
    int getQuality();

    /**
     * @return expression evaluation context containing {@code item.quality}, player variables, and resolvers
     */
    EvaluationContext getEvaluationContext();

    /**
     * Unwraps an optional platform-specific object (such as Paper {@code Player}) if available.
     *
     * @param handleType expected platform type
     * @param <T>        type parameter
     * @return optional containing the platform handle, or empty in pure-core environments
     */
    default <T> Optional<T> getPlatformHandle(Class<T> handleType) {
        return Optional.empty();
    }
}
