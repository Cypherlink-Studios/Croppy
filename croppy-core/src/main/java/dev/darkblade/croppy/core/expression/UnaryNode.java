package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;

import java.util.Objects;

/**
 * AST node evaluating unary operations (NOT, MINUS).
 */
public record UnaryNode(TokenType operator, ExpressionNode target) implements ExpressionNode {

    public UnaryNode {
        Objects.requireNonNull(operator, "operator cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
    }

    @Override
    public Object evaluateValue(EvaluationContext context) {
        if (operator == TokenType.NOT) {
            return !target.evaluate(context);
        } else if (operator == TokenType.MINUS) {
            Object val = target.evaluateValue(context);
            if (val instanceof Number n) {
                return -n.doubleValue();
            }
            if (val != null) {
                try {
                    return -Double.parseDouble(val.toString());
                } catch (NumberFormatException ignored) {}
            }
            return 0.0;
        }
        throw new UnsupportedOperationException("Operator " + operator + " is not a valid unary operator");
    }

    @Override
    public boolean evaluate(EvaluationContext context) {
        if (operator == TokenType.NOT) {
            return !target.evaluate(context);
        }
        return ExpressionNode.super.evaluate(context);
    }
}
