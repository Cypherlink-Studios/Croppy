package dev.darkblade.croppy.core.consumable;

import dev.darkblade.croppy.api.consumable.ConsumeExecutionContext;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link ConsumeExecutionContext} supporting optional platform handle unwrapping.
 */
public final class SimpleConsumeExecutionContext implements ConsumeExecutionContext {

    private final UUID playerId;
    private final String playerName;
    private final ItemDefinition itemDefinition;
    private final StoredItem storedItem;
    private final int quality;
    private final EvaluationContext evaluationContext;
    private final Object platformHandle;

    public SimpleConsumeExecutionContext(
            UUID playerId,
            String playerName,
            ItemDefinition itemDefinition,
            StoredItem storedItem,
            int quality,
            EvaluationContext evaluationContext,
            Object platformHandle
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.playerName = playerName != null ? playerName : "";
        this.itemDefinition = Objects.requireNonNull(itemDefinition, "itemDefinition cannot be null");
        this.storedItem = storedItem;
        this.quality = Math.max(0, Math.min(3, quality));
        this.evaluationContext = evaluationContext != null ? evaluationContext : EvaluationContext.empty();
        this.platformHandle = platformHandle;
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    @Override
    public Optional<StoredItem> getStoredItem() {
        return Optional.ofNullable(storedItem);
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    public EvaluationContext getEvaluationContext() {
        return evaluationContext;
    }

    @Override
    public <T> Optional<T> getPlatformHandle(Class<T> handleType) {
        if (handleType != null && handleType.isInstance(platformHandle)) {
            return Optional.of(handleType.cast(platformHandle));
        }
        return Optional.empty();
    }
}
