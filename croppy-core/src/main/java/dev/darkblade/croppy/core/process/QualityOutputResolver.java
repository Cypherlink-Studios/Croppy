package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.OutputResolver;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link OutputResolver} dynamically computing output item
 * quantities and quality ratings based on ingredient input quality and execution accuracy.
 */
public final class QualityOutputResolver implements OutputResolver {

    private final Key resultItemKey;
    private final int resultAmount;

    public QualityOutputResolver(Key resultItemKey, int resultAmount) {
        this.resultItemKey = Objects.requireNonNull(resultItemKey, "resultItemKey cannot be null");
        this.resultAmount = Math.max(1, resultAmount);
    }

    @Override
    public List<StoredItem> resolveOutputs(ProcessDefinition definition, ProcessInstance instance, List<StoredItem> originalInputs) {
        int quality = calculateQuality(instance, originalInputs);
        return List.of(StoredItem.of(resultItemKey, resultAmount, quality));
    }

    /**
     * Calculates the resulting quality grade (0: Normal, 1: Silver, 2: Gold, 3: Iridium).
     *
     * @param instance runtime process state
     * @param inputs   initial input ingredients
     * @return calculated quality integer clamped to [0, 3]
     */
    public static int calculateQuality(ProcessInstance instance, List<StoredItem> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return 0;
        }

        double totalQuality = 0;
        int count = 0;
        for (StoredItem item : inputs) {
            totalQuality += ((double) item.quality() * item.amount());
            count += item.amount();
        }

        double baseQuality = (count > 0) ? (totalQuality / count) : 0.0;

        // Apply bonus from execution performance if recorded in context
        Object bonusObj = instance.getContextVariables().get("quality_bonus");
        double bonus = (bonusObj instanceof Number n) ? n.doubleValue() : 0.0;

        int finalQuality = (int) Math.round(baseQuality + bonus);
        return Math.max(0, Math.min(3, finalQuality));
    }
}
