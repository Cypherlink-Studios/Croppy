package dev.darkblade.croppy.api.expression;

/**
 * Represents a compiled boolean condition or expression that can be evaluated against
 * an {@link EvaluationContext}.
 */
@FunctionalInterface
public interface Expression {

    /**
     * Evaluates the expression against the provided evaluation context.
     *
     * @param context the context providing variables and resolvers
     * @return true if the condition evaluates to true, false otherwise
     */
    boolean evaluate(EvaluationContext context);

    /**
     * An expression that always evaluates to true.
     */
    Expression ALWAYS_TRUE = context -> true;

    /**
     * An expression that always evaluates to false.
     */
    Expression ALWAYS_FALSE = context -> false;
}
