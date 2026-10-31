package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;

import java.util.Objects;

/**
 * AST node evaluating arithmetic operations (+, -, *, /) between sub-expressions.
 */
public record ArithmeticNode(ExpressionNode left, TokenType operator, ExpressionNode right) implements ExpressionNode {

    public ArithmeticNode {
        Objects.requireNonNull(left, "left cannot be null");
        Objects.requireNonNull(operator, "operator cannot be null");
        Objects.requireNonNull(right, "right cannot be null");
    }

    @Override
    public Object evaluateValue(EvaluationContext context) {
        Object leftVal = left.evaluateValue(context);
        Object rightVal = right.evaluateValue(context);

        // String concatenation with +
        if (operator == TokenType.PLUS && (leftVal instanceof String || rightVal instanceof String)) {
            return String.valueOf(leftVal) + String.valueOf(rightVal);
        }

        double l = toDouble(leftVal);
        double r = toDouble(rightVal);

        return switch (operator) {
            case PLUS -> l + r;
            case MINUS -> l - r;
            case STAR -> l * r;
            case SLASH -> {
                if (r == 0.0) {
                    throw new ArithmeticException("Division by zero in expression");
                }
                yield l / r;
            }
            default -> throw new UnsupportedOperationException("Operator " + operator + " is not a valid arithmetic operator");
        };
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}
