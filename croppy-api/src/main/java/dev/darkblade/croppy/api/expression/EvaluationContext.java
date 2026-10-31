package dev.darkblade.croppy.api.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides variable resolution and evaluation scope for {@link Expression} instances.
 */
public interface EvaluationContext {

    /**
     * Resolves a variable by its name.
     *
     * @param name the variable name (e.g., "temperature", "station.fuel", "%player_name%")
     * @return an optional containing the resolved value if present
     */
    Optional<Object> getVariable(String name);

    /**
     * Returns a new context containing the specified variable binding.
     *
     * @param key   the variable key
     * @param value the variable value
     * @return a new context with the variable added
     */
    EvaluationContext with(String key, Object value);

    /**
     * Returns a new context including an additional variable resolver.
     *
     * @param resolver the resolver to register
     * @return a new context with the resolver added
     */
    EvaluationContext withResolver(VariableResolver resolver);

    /**
     * Creates an empty evaluation context.
     *
     * @return an empty context
     */
    static EvaluationContext empty() {
        return of(Map.of());
    }

    /**
     * Creates an evaluation context backed by the provided variable map.
     *
     * @param variables map of local variables
     * @return a new evaluation context
     */
    static EvaluationContext of(Map<String, Object> variables) {
        return new SimpleEvaluationContext(Map.copyOf(variables), List.of());
    }

    /**
     * Internal immutable record implementing the default evaluation context.
     */
    record SimpleEvaluationContext(Map<String, Object> variables, List<VariableResolver> resolvers) implements EvaluationContext {

        public SimpleEvaluationContext {
            variables = Map.copyOf(variables);
            resolvers = List.copyOf(resolvers);
        }

        @Override
        public Optional<Object> getVariable(String name) {
            Objects.requireNonNull(name, "name cannot be null");
            if (variables.containsKey(name)) {
                return Optional.ofNullable(variables.get(name));
            }
            for (VariableResolver resolver : resolvers) {
                Optional<Object> resolved = resolver.resolve(name, this);
                if (resolved.isPresent()) {
                    return resolved;
                }
            }
            return Optional.empty();
        }

        @Override
        public EvaluationContext with(String key, Object value) {
            Objects.requireNonNull(key, "key cannot be null");
            Map<String, Object> newVars = new HashMap<>(variables);
            if (value == null) {
                newVars.remove(key);
            } else {
                newVars.put(key, value);
            }
            return new SimpleEvaluationContext(newVars, resolvers);
        }

        @Override
        public EvaluationContext withResolver(VariableResolver resolver) {
            Objects.requireNonNull(resolver, "resolver cannot be null");
            var newResolvers = new java.util.ArrayList<>(resolvers);
            newResolvers.add(resolver);
            return new SimpleEvaluationContext(variables, newResolvers);
        }
    }
}
