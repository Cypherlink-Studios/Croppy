package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;

/**
 * Node within the abstract syntax tree of a compiled expression.
 */
public interface ExpressionNode extends Expression {

    /**
     * Evaluates the node against the context, returning the resulting value.
     *
     * @param context the evaluation context
     * @return the evaluated value (Boolean, Number, String, or null)
     */
    Object evaluateValue(EvaluationContext context);

    @Override
    default boolean evaluate(EvaluationContext context) {
        Object val = evaluateValue(context);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        return val != null;
    }
}
