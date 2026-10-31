package dev.darkblade.croppy.api.process;

import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;

/**
 * Functional resolver calculating the final output items and quality ratings upon process completion.
 */
@FunctionalInterface
public interface OutputResolver {

    /**
     * Resolves the list of output items to produce upon completing the given process instance.
     *
     * @param definition     the process definition being executed
     * @param instance       runtime state and recorded variables
     * @param originalInputs inputs initially provided to start the process
     * @return list of produced items with calculated quantities and quality grades
     */
    List<StoredItem> resolveOutputs(ProcessDefinition definition, ProcessInstance instance, List<StoredItem> originalInputs);
}
