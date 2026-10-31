package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.menu.ConditionalTemplate;
import dev.darkblade.croppy.api.menu.MenuAction;
import dev.darkblade.croppy.api.menu.MenuItemTemplate;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;

import java.util.Objects;
import java.util.Optional;

/**
 * Utility evaluating conditional menu slot templates and action gates against runtime {@link EvaluationContext} scopes.
 */
public final class MenuExpressionEvaluator {

    private MenuExpressionEvaluator() {}

    /**
     * Evaluates a boolean condition expression against the provided context.
     *
     * @param condition expression string (e.g., "process.state == 'RUNNING'")
     * @param context   evaluation context providing variables and resolvers
     * @return true if condition is empty or evaluates to true; false on failure or evaluation exception
     */
    public static boolean evaluateCondition(String condition, EvaluationContext context) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        EvaluationContext evalCtx = (context == null) ? EvaluationContext.empty() : context;
        try {
            Expression expr = ExpressionCompiler.compile(condition);
            return expr.evaluate(evalCtx);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Resolves the active visual {@link MenuItemTemplate} for a slot definition by checking its conditional
     * states in priority order, falling back to the default template if none match.
     *
     * @param slot    the slot definition
     * @param context the evaluation context
     * @return optional active template
     */
    public static Optional<MenuItemTemplate> resolveTemplate(SlotDefinition slot, EvaluationContext context) {
        Objects.requireNonNull(slot, "slot cannot be null");
        for (ConditionalTemplate state : slot.states()) {
            if (evaluateCondition(state.condition(), context)) {
                return Optional.of(state.template());
            }
        }
        return slot.getDefaultTemplate();
    }

    /**
     * Determines whether an action is permitted to execute given its condition expression.
     *
     * @param action  the menu action
     * @param context the evaluation context
     * @return true if permitted; false otherwise
     */
    public static boolean isActionAllowed(MenuAction action, EvaluationContext context) {
        Objects.requireNonNull(action, "action cannot be null");
        if (action.getCondition().isEmpty()) {
            return true;
        }
        return evaluateCondition(action.getCondition().get(), context);
    }
}
