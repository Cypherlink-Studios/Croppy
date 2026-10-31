package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;

import java.util.Collection;
import java.util.Objects;

/**
 * AST node evaluating comparison relationships between two sub-expressions.
 */
public record ComparisonNode(ExpressionNode left, TokenType operator, ExpressionNode right) implements ExpressionNode {

    public ComparisonNode {
        Objects.requireNonNull(left, "left cannot be null");
        Objects.requireNonNull(operator, "operator cannot be null");
        Objects.requireNonNull(right, "right cannot be null");
    }

    @Override
    public Object evaluateValue(EvaluationContext context) {
        Object leftVal = left.evaluateValue(context);
        Object rightVal = right.evaluateValue(context);

        return switch (operator) {
            case EQUAL -> evaluateEquals(leftVal, rightVal);
            case NOT_EQUAL -> !evaluateEquals(leftVal, rightVal);
            case LESS_THAN -> compare(leftVal, rightVal) < 0;
            case LESS_THAN_OR_EQUAL -> compare(leftVal, rightVal) <= 0;
            case GREATER_THAN -> compare(leftVal, rightVal) > 0;
            case GREATER_THAN_OR_EQUAL -> compare(leftVal, rightVal) >= 0;
            case CONTAINS -> evaluateContains(leftVal, rightVal);
            case IN -> evaluateContains(rightVal, leftVal);
            default -> throw new UnsupportedOperationException("Operator " + operator + " is not a valid comparison operator");
        };
    }

    private boolean evaluateEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }

        if (a instanceof Boolean ba && b instanceof Boolean bb) {
            return ba.equals(bb);
        }

        // String equality or string vs number/boolean representation
        return a.toString().equalsIgnoreCase(b.toString());
    }

    private int compare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }

        // If one is string representation of number
        try {
            double da = Double.parseDouble(a.toString());
            double db = Double.parseDouble(b.toString());
            return Double.compare(da, db);
        } catch (NumberFormatException ignored) {
            // Compare as strings
            return a.toString().compareToIgnoreCase(b.toString());
        }
    }

    private boolean evaluateContains(Object container, Object item) {
        if (container == null || item == null) return false;

        if (container instanceof Collection<?> col) {
            return col.contains(item) || col.stream().anyMatch(e -> evaluateEquals(e, item));
        }

        return container.toString().contains(item.toString());
    }
}
