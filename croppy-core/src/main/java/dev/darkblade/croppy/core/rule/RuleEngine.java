package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Core engine evaluating declarative {@link RuleDefinition} models and applying gameplay consequences.
 */
public final class RuleEngine {

    private final RuleRegistry ruleRegistry;

    public RuleEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry cannot be null");
    }

    /**
     * Evaluates all rules bound to the specified trigger and executes consequences on matching conditions.
     * For {@link RuleTrigger#DISPLAY_LAYOUT}, evaluates with first-match semantics (highest priority matching rule wins).
     *
     * @param trigger the event trigger
     * @param station the target station instance
     * @param context evaluation context for Pratt AST expressions
     * @return list of executed rules and actions
     */
    public List<RuleExecution> evaluateAndApply(RuleTrigger trigger, StationInstance station, EvaluationContext context) {
        Objects.requireNonNull(trigger, "trigger cannot be null");
        Objects.requireNonNull(station, "station cannot be null");
        EvaluationContext evalCtx = (context == null) ? EvaluationContext.empty() : context;

        Key stationKey = (station.getDefinition() != null) ? station.getDefinition().getKey() : null;
        List<RuleDefinition> rules = ruleRegistry.getRules(trigger, stationKey);
        List<RuleExecution> executed = new ArrayList<>();
        boolean isFirstMatchOnly = (trigger == RuleTrigger.DISPLAY_LAYOUT);

        for (RuleDefinition rule : rules) {
            boolean conditionsMet = true;
            for (String condition : rule.conditions()) {
                try {
                    Expression expr = ExpressionCompiler.compile(condition);
                    if (!expr.evaluate(evalCtx)) {
                        conditionsMet = false;
                        break;
                    }
                } catch (Exception ex) {
                    conditionsMet = false;
                    break;
                }
            }

            if (!conditionsMet) {
                continue;
            }

            // Apply in-memory domain state consequences
            applyDomainActions(station, rule.actions());
            executed.add(new RuleExecution(rule, rule.actions()));

            if (isFirstMatchOnly) {
                break;
            }
        }

        return executed;
    }

    private void applyDomainActions(StationInstance station, List<RuleAction> actions) {
        for (RuleAction action : actions) {
            String type = action.type().toUpperCase();
            if ("REPLACE_OUTPUT".equals(type)) {
                applyReplaceOutput(station, action);
            } else if ("REDUCE_QUALITY".equals(type)) {
                applyReduceQuality(station, action);
            }
        }
    }

    private void applyReplaceOutput(StationInstance station, RuleAction action) {
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        StorageComponent storage = storageOpt.get();
        Object target = action.parameters().get("target_item");
        if (target == null) {
            target = action.parameters().get("item");
        }
        if (target == null) {
            return;
        }

        String targetStr = target.toString().trim();
        Key targetKey = targetStr.contains(":") ? Key.parse(targetStr) : Key.of("croppy", targetStr);

        List<StoredItem> current = storage.extractOutputs();
        int totalAmount = current.stream().mapToInt(StoredItem::amount).sum();
        if (totalAmount <= 0) {
            totalAmount = 1;
        }

        storage.addOutput(StoredItem.of(targetKey, totalAmount, 0));
    }

    private void applyReduceQuality(StationInstance station, RuleAction action) {
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        StorageComponent storage = storageOpt.get();
        int dropAmount = 1;
        Object rawAmount = action.parameters().get("amount");
        if (rawAmount instanceof Number n) {
            dropAmount = n.intValue();
        }

        List<StoredItem> current = storage.extractOutputs();
        for (StoredItem item : current) {
            int newQuality = Math.max(0, item.quality() - dropAmount);
            storage.addOutput(StoredItem.of(item.itemKey(), item.amount(), newQuality));
        }
    }
}
