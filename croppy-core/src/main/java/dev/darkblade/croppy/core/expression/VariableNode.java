package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;

import java.util.Objects;

/**
 * AST node that resolves a variable dynamically from the {@link EvaluationContext}.
 *
 * @param variableName the name of the variable to resolve
 */
public record VariableNode(String variableName) implements ExpressionNode {

    public VariableNode {
        Objects.requireNonNull(variableName, "variableName cannot be null");
    }

    @Override
    public Object evaluateValue(EvaluationContext context) {
        return context.getVariable(variableName).orElse(null);
    }
}
