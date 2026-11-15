package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.variable.DataType;
import dev.darkblade.croppy.api.variable.OperationArgs;
import dev.darkblade.croppy.api.variable.VariableOperation;

import java.util.Objects;

/**
 * Factory utilities for standard {@link VariableOperation} implementations.
 */
public final class StandardOperations {

    private StandardOperations() {
    }

    public static <T> VariableOperation<T> set(DataType<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "SET";
            }

            @Override
            public T apply(T currentValue, OperationArgs args) {
                Object raw = args.getRequired("value");
                return type.parse(raw);
            }
        };
    }

    public static VariableOperation<Long> addLong() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "ADD";
            }

            @Override
            public Long apply(Long currentValue, OperationArgs args) {
                long delta = extractLongDelta(args);
                long base = currentValue != null ? currentValue : 0L;
                return Math.addExact(base, delta);
            }
        };
    }

    public static VariableOperation<Long> subtractLong() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "SUBTRACT";
            }

            @Override
            public Long apply(Long currentValue, OperationArgs args) {
                long delta = extractLongDelta(args);
                long base = currentValue != null ? currentValue : 0L;
                return Math.subtractExact(base, delta);
            }
        };
    }

    public static VariableOperation<Long> multiplyLong() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "MULTIPLY";
            }

            @Override
            public Long apply(Long currentValue, OperationArgs args) {
                long factor = extractLongDelta(args);
                long base = currentValue != null ? currentValue : 0L;
                return Math.multiplyExact(base, factor);
            }
        };
    }

    public static VariableOperation<Long> clampLong() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "CLAMP";
            }

            @Override
            public Long apply(Long currentValue, OperationArgs args) {
                long base = currentValue != null ? currentValue : 0L;
                long min = args.getLong("min").orElse(Long.MIN_VALUE);
                long max = args.getLong("max").orElse(Long.MAX_VALUE);
                if (min > max) {
                    throw new IllegalArgumentException("CLAMP min (" + min + ") cannot exceed max (" + max + ")");
                }
                return Math.clamp(base, min, max);
            }
        };
    }

    public static VariableOperation<Double> addDouble() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "ADD";
            }

            @Override
            public Double apply(Double currentValue, OperationArgs args) {
                double delta = extractDoubleDelta(args);
                double base = currentValue != null ? currentValue : 0.0;
                return base + delta;
            }
        };
    }

    public static VariableOperation<Double> subtractDouble() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "SUBTRACT";
            }

            @Override
            public Double apply(Double currentValue, OperationArgs args) {
                double delta = extractDoubleDelta(args);
                double base = currentValue != null ? currentValue : 0.0;
                return base - delta;
            }
        };
    }

    public static VariableOperation<Double> multiplyDouble() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "MULTIPLY";
            }

            @Override
            public Double apply(Double currentValue, OperationArgs args) {
                double factor = extractDoubleDelta(args);
                double base = currentValue != null ? currentValue : 0.0;
                return base * factor;
            }
        };
    }

    public static VariableOperation<Double> clampDouble() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "CLAMP";
            }

            @Override
            public Double apply(Double currentValue, OperationArgs args) {
                double base = currentValue != null ? currentValue : 0.0;
                double min = args.getDouble("min").orElse(-Double.MAX_VALUE);
                double max = args.getDouble("max").orElse(Double.MAX_VALUE);
                if (min > max) {
                    throw new IllegalArgumentException("CLAMP min (" + min + ") cannot exceed max (" + max + ")");
                }
                return Math.clamp(base, min, max);
            }
        };
    }

    public static VariableOperation<Boolean> toggleBoolean() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "TOGGLE";
            }

            @Override
            public Boolean apply(Boolean currentValue, OperationArgs args) {
                boolean base = currentValue != null && currentValue;
                return !base;
            }
        };
    }

    public static VariableOperation<String> appendString() {
        return new VariableOperation<>() {
            @Override
            public String getId() {
                return "APPEND";
            }

            @Override
            public String apply(String currentValue, OperationArgs args) {
                String suffix = args.getString("value")
                        .or(() -> args.getString("text"))
                        .orElseThrow(() -> new IllegalArgumentException("Missing 'value' or 'text' argument for APPEND"));
                String base = currentValue != null ? currentValue : "";
                return base + suffix;
            }
        };
    }

    private static long extractLongDelta(OperationArgs args) {
        return args.getLong("amount")
                .or(() -> args.getLong("value"))
                .orElseThrow(() -> new IllegalArgumentException("Missing 'amount' or 'value' numeric argument"));
    }

    private static double extractDoubleDelta(OperationArgs args) {
        return args.getDouble("amount")
                .or(() -> args.getDouble("value"))
                .orElseThrow(() -> new IllegalArgumentException("Missing 'amount' or 'value' numeric argument"));
    }
}
