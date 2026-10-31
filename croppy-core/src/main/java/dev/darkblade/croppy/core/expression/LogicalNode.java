package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;

import java.util.Objects;

/**
 * AST node evaluating short-circuit logical boolean operations (AND, OR).
 */
public record LogicalNode(ExpressionNode left, TokenType operator, ExpressionNode right) implements ExpressionNode {

    public LogicalNode {
        Objects.requireNonNull(left, "left cannot be null");
        Objects.requireNonNull(operator, "operator cannot be null");
        Objects.requireNonNull(right, "right cannot be null");
    }

    @Override
    public Object evaluateValue(EvaluationContext context) {
        return evaluate(context);
    }

    @Override
    public boolean evaluate(EvaluationContext context) {
        if (operator == TokenType.AND) {
            if (!left.evaluate(context)) {
                return false;
            }
            return right.evaluate(context);
        } else if (operator == TokenType.OR) {
            if (left.evaluate(context)) {
                return true;
            }
            return right.evaluate(context);
        }
        throw new UnsupportedOperationException("Operator " + operator + " is not a valid logical operator");
    }
}
