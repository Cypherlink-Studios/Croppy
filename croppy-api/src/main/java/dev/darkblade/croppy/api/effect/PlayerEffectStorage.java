package dev.darkblade.croppy.api.effect;

import java.util.List;
import java.util.UUID;

/**
 * Port for loading and saving persistent player status effects.
 */
public interface PlayerEffectStorage {

    List<ActiveStatusEffect> load(UUID playerId);

    void save(UUID playerId, List<ActiveStatusEffect> effects);

    void clear(UUID playerId);
}
