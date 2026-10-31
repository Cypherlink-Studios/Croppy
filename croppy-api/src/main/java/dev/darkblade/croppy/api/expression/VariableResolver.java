package dev.darkblade.croppy.api.expression;

import java.util.Optional;

/**
 * Port interface for dynamically resolving variable values during expression evaluation.
 */
@FunctionalInterface
public interface VariableResolver {

    /**
     * Resolves the variable by name within the provided context.
     *
     * @param name    the variable name to resolve
     * @param context the current evaluation context
     * @return an optional containing the resolved value, or empty if this resolver cannot handle it
     */
    Optional<Object> resolve(String name, EvaluationContext context);
}
