package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.Comparator;
import java.util.List;

/**
 * Default in-memory implementation of {@link RuleRegistry}.
 */
public final class DefaultRuleRegistry extends SimpleRegistry<RuleDefinition> implements RuleRegistry {

    private static final Comparator<RuleDefinition> PRIORITY_COMPARATOR =
            Comparator.comparingInt(RuleDefinition::priority).reversed();

    @Override
    public List<RuleDefinition> getRulesByTrigger(RuleTrigger trigger) {
        if (trigger == null) {
            return List.of();
        }
        return values().stream()
                .filter(rule -> rule.trigger() == trigger)
                .sorted(PRIORITY_COMPARATOR)
                .toList();
    }
}
