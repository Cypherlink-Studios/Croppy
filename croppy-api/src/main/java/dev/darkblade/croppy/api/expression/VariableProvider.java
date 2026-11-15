package dev.darkblade.croppy.api.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Port interface for domain components, variable stores, or entities that contribute
 * contextual variables under a dedicated namespace to an {@link EvaluationContext}.
 */
public interface VariableProvider {

    /**
     * Returns the namespace prefix for variables exported by this provider
     * (e.g., {@code "thermal"}, {@code "heat"}, {@code "storage"}, {@code "process"}, {@code "var"}).
     *
     * @return non-blank namespace identifier
     */
    String getVariableNamespace();

    /**
     * Populates variable keys (relative to {@link #getVariableNamespace()}) and their values
     * into the provided map. Nested structures should use dot-notation keys (e.g., {@code "tank.fluid"}).
     *
     * @param variables target map receiving relative variable keys and values
     */
    void populateVariables(Map<String, Object> variables);

    /**
     * Exports all variables from this provider into {@code target}, automatically prefixing
     * each key with {@link #getVariableNamespace()} followed by a dot.
     *
     * @param target destination map receiving namespaced variables
     */
    default void exportNamespaced(Map<String, Object> target) {
        Objects.requireNonNull(target, "target cannot be null");
        String namespace = getVariableNamespace();
        Map<String, Object> local = new HashMap<>();
        populateVariables(local);
        if (namespace == null || namespace.isBlank()) {
            target.putAll(local);
            return;
        }
        String prefix = namespace.endsWith(".") ? namespace : namespace + ".";
        for (Map.Entry<String, Object> entry : local.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                target.put(prefix + entry.getKey(), entry.getValue());
            }
        }
    }
}
