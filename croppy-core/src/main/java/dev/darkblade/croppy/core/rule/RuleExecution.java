package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Record representing a triggered rule and its executed actions.
 *
 * @param rule           the triggered rule definition
 * @param appliedActions the actions applied or queued for platform execution
 */
public record RuleExecution(RuleDefinition rule, List<RuleAction> appliedActions) {

    public RuleExecution {
        Objects.requireNonNull(rule, "rule cannot be null");
        appliedActions = (appliedActions == null) ? List.of() : List.copyOf(appliedActions);
    }
}
