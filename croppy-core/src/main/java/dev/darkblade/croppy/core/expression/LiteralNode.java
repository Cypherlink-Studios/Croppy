package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;

/**
 * AST node representing a literal constant value (Number, Boolean, String, null).
 *
 * @param value the literal constant value
 */
public record LiteralNode(Object value) implements ExpressionNode {

    @Override
    public Object evaluateValue(EvaluationContext context) {
        return value;
    }
}
