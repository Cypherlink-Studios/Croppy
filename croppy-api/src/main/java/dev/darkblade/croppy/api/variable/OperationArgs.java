package dev.darkblade.croppy.api.variable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable argument container passed to {@link VariableOperation} executions.
 */
public final class OperationArgs {

    public static final OperationArgs EMPTY = new OperationArgs(Map.of());

    private final Map<String, Object> parameters;

    private OperationArgs(Map<String, Object> parameters) {
        this.parameters = Map.copyOf(parameters);
    }

    /**
     * Creates an {@link OperationArgs} instance from a parameter map.
     *
     * @param parameters parameter map
     * @return immutable operation args
     */
    public static OperationArgs of(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return EMPTY;
        }
        return new OperationArgs(parameters);
    }

    /**
     * Convenience factory for a single key-value argument.
     *
     * @param key   argument key
     * @param value argument value
     * @return immutable operation args
     */
    public static OperationArgs of(String key, Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value == null) {
            return EMPTY;
        }
        return new OperationArgs(Map.of(key, value));
    }

    public Optional<Object> get(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable(parameters.get(key));
    }

    public Object getRequired(String key) {
        return get(key).orElseThrow(() ->
                new IllegalArgumentException("Missing required operation argument: '" + key + "'"));
    }

    public Optional<String> getString(String key) {
        return get(key).map(Object::toString);
    }

    public Optional<Long> getLong(String key) {
        return get(key).map(val -> {
            if (val instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(val.toString().trim());
        });
    }

    public Optional<Double> getDouble(String key) {
        return get(key).map(val -> {
            if (val instanceof Number n) {
                return n.doubleValue();
            }
            return Double.parseDouble(val.toString().trim());
        });
    }

    public Optional<Boolean> getBoolean(String key) {
        return get(key).map(val -> {
            if (val instanceof Boolean b) {
                return b;
            }
            String str = val.toString().trim().toLowerCase();
            if ("true".equals(str)) return true;
            if ("false".equals(str)) return false;
            throw new IllegalArgumentException("Cannot parse boolean from argument '" + key + "': " + val);
        });
    }

    public Map<String, Object> asMap() {
        return parameters;
    }
}
