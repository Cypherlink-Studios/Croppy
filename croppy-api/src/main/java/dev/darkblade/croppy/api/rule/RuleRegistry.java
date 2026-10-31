package dev.darkblade.croppy.api.rule;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

import java.util.List;

/**
 * Registry containing all registered {@link RuleDefinition} configurations.
 */
public interface RuleRegistry extends Registry<RuleDefinition>, Service {

    /**
     * Retrieves all rule definitions associated with a specific trigger, sorted by priority descending.
     *
     * @param trigger the rule trigger
     * @return priority-sorted rule definitions
     */
    List<RuleDefinition> getRulesByTrigger(RuleTrigger trigger);
}
