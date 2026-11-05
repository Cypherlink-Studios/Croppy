package dev.darkblade.croppy.core.interaction;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.Comparator;
import java.util.List;

/**
 * Default in-memory implementation of {@link InteractionRegistry}.
 */
public final class DefaultInteractionRegistry extends SimpleRegistry<InteractionDefinition> implements InteractionRegistry {

    private static final Comparator<InteractionDefinition> PRIORITY_COMPARATOR =
            Comparator.comparingInt(InteractionDefinition::priority).reversed();

    @Override
    public List<InteractionDefinition> getInteractionsForStation(Key stationKey) {
        return values().stream()
                .filter(def -> def.targetCrop().isEmpty()) // station interactions have no targetCrop
                .filter(def -> def.targetStation().isEmpty() || (stationKey != null && def.targetStation().get().equals(stationKey)))
                .sorted(PRIORITY_COMPARATOR)
                .toList();
    }

    @Override
    public List<InteractionDefinition> getInteractionsForCrop(Key cropKey) {
        return values().stream()
                .filter(def -> def.targetCrop().isPresent())
                .filter(def -> def.targetCrop().get().equals(cropKey))
                .sorted(PRIORITY_COMPARATOR)
                .toList();
    }
}
