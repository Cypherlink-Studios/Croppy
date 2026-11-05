package dev.darkblade.croppy.api.species;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the harvest yield drops and optional cyclic regrowth policy for a mature crop species.
 *
 * @param yields   list of drop rules
 * @param regrowth optional cyclic regrowth definition
 */
public record HarvestDefinition(List<YieldEntry> yields, Optional<RegrowthDefinition> regrowth) {

    public HarvestDefinition {
        Objects.requireNonNull(yields, "Yields cannot be null");
        Objects.requireNonNull(regrowth, "Regrowth cannot be null");
        yields = Collections.unmodifiableList(yields);
    }

    public HarvestDefinition(List<YieldEntry> yields) {
        this(yields, Optional.empty());
    }

    public HarvestDefinition(List<YieldEntry> yields, RegrowthDefinition regrowth) {
        this(yields, Optional.ofNullable(regrowth));
    }

    public boolean hasRegrowth() {
        return regrowth.isPresent();
    }

    public static HarvestDefinition of(YieldEntry... entries) {
        return new HarvestDefinition(List.of(entries), Optional.empty());
    }
}
