package dev.darkblade.croppy.api.species;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines the harvest yield drops for a mature crop species.
 *
 * @param yields list of drop rules
 */
public record HarvestDefinition(List<YieldEntry> yields) {
    public HarvestDefinition {
        Objects.requireNonNull(yields, "Yields cannot be null");
        yields = Collections.unmodifiableList(yields);
    }

    public static HarvestDefinition of(YieldEntry... entries) {
        return new HarvestDefinition(List.of(entries));
    }
}
