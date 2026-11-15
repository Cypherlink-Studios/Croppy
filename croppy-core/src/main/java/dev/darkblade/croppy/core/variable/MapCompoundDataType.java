package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.variable.CompoundDataType;
import dev.darkblade.croppy.api.variable.OperationArgs;
import dev.darkblade.croppy.api.variable.VariableOperation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Standard composite data type ({@code croppy:compound}) storing nested key-value structures
 * and indexed lists, with dot-notation property export and traversal.
 */
@SuppressWarnings("unchecked")
public final class MapCompoundDataType implements CompoundDataType<Map<String, Object>> {

    public static final Key KEY = Key.of("croppy", "compound");
    public static final MapCompoundDataType INSTANCE = new MapCompoundDataType();

    private static final byte TAG_NULL = 0;
    private static final byte TAG_LONG = 1;
    private static final byte TAG_DOUBLE = 2;
    private static final byte TAG_BOOLEAN = 3;
    private static final byte TAG_STRING = 4;
    private static final byte TAG_MAP = 5;
    private static final byte TAG_LIST = 6;

    private final List<VariableOperation<Map<String, Object>>> operations;

    private MapCompoundDataType() {
        this.operations = List.of(
                StandardOperations.set(this),
                new PutOperation(),
                new RemoveOperation(),
                new ClearOperation()
        );
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<Map<String, Object>> getValueClass() {
        return (Class) Map.class;
    }

    @Override
    public Map<String, Object> getDefaultValue() {
        return Map.of();
    }

    @Override
    public Map<String, Object> parse(Object rawInput) {
        if (rawInput == null) {
            return Map.of();
        }
        if (!(rawInput instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("Compound value must be a key-value map, got: " + rawInput.getClass().getSimpleName());
        }
        return normalizeMap(rawMap);
    }

    @Override
    public Optional<Object> getProperty(Map<String, Object> value, String propertyPath) {
        if (value == null || propertyPath == null || propertyPath.isBlank()) {
            return Optional.empty();
        }
        String[] segments = propertyPath.split("\\.");
        Object current = value;
        for (String segment : segments) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(segment);
            } else if (current instanceof List<?> list) {
                try {
                    int idx = Integer.parseInt(segment);
                    if (idx < 0 || idx >= list.size()) {
                        return Optional.empty();
                    }
                    current = list.get(idx);
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    @Override
    public void exportVariables(String variableName, Map<String, Object> value, Map<String, Object> destination) {
        if (variableName == null || value == null) {
            return;
        }
        destination.put(variableName, value);
        exportEntries(variableName, value, destination);
    }

    private void exportEntries(String prefix, Object current, Map<String, Object> destination) {
        if (current instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String key = prefix + "." + entry.getKey();
                Object val = entry.getValue();
                destination.put(key, val);
                exportEntries(key, val, destination);
            }
        } else if (current instanceof List<?> list) {
            destination.put(prefix + ".size", list.size());
            for (int i = 0; i < list.size(); i++) {
                Object val = list.get(i);
                if (val == null) {
                    continue;
                }
                String key = prefix + "." + i;
                destination.put(key, val);
                exportEntries(key, val, destination);
            }
        }
    }

    @Override
    public void writeBinary(DataOutput out, Map<String, Object> value) throws IOException {
        Map<String, Object> safe = (value != null) ? value : Map.of();
        writeMapBinary(out, safe);
    }

    @Override
    public Map<String, Object> readBinary(DataInput in) throws IOException {
        return readMapBinary(in);
    }

    @Override
    public Collection<VariableOperation<Map<String, Object>>> getSupportedOperations() {
        return operations;
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Object normalizeValue(Object val) {
        if (val instanceof Integer || val instanceof Long || val instanceof Short || val instanceof Byte) {
            return ((Number) val).longValue();
        }
        if (val instanceof Float || val instanceof Double) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof Boolean) {
            return val;
        }
        if (val instanceof Map<?, ?> nested) {
            return normalizeMap(nested);
        }
        if (val instanceof List<?> list) {
            List<Object> normalizedList = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) {
                    normalizedList.add(normalizeValue(item));
                }
            }
            return Collections.unmodifiableList(normalizedList);
        }
        return val.toString();
    }

    private static void writeMapBinary(DataOutput out, Map<?, ?> map) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            out.writeUTF(entry.getKey().toString());
            writeValueBinary(out, entry.getValue());
        }
    }

    private static void writeValueBinary(DataOutput out, Object val) throws IOException {
        if (val == null) {
            out.writeByte(TAG_NULL);
        } else if (val instanceof Long || val instanceof Integer || val instanceof Short || val instanceof Byte) {
            out.writeByte(TAG_LONG);
            out.writeLong(((Number) val).longValue());
        } else if (val instanceof Double || val instanceof Float) {
            out.writeByte(TAG_DOUBLE);
            out.writeDouble(((Number) val).doubleValue());
        } else if (val instanceof Boolean b) {
            out.writeByte(TAG_BOOLEAN);
            out.writeBoolean(b);
        } else if (val instanceof Map<?, ?> m) {
            out.writeByte(TAG_MAP);
            writeMapBinary(out, m);
        } else if (val instanceof List<?> l) {
            out.writeByte(TAG_LIST);
            out.writeInt(l.size());
            for (Object item : l) {
                writeValueBinary(out, item);
            }
        } else {
            out.writeByte(TAG_STRING);
            out.writeUTF(val.toString());
        }
    }

    private static Map<String, Object> readMapBinary(DataInput in) throws IOException {
        int size = in.readInt();
        Map<String, Object> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            Object val = readValueBinary(in);
            if (val != null) {
                map.put(key, val);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Object readValueBinary(DataInput in) throws IOException {
        byte tag = in.readByte();
        return switch (tag) {
            case TAG_NULL -> null;
            case TAG_LONG -> in.readLong();
            case TAG_DOUBLE -> in.readDouble();
            case TAG_BOOLEAN -> in.readBoolean();
            case TAG_STRING -> in.readUTF();
            case TAG_MAP -> readMapBinary(in);
            case TAG_LIST -> {
                int len = in.readInt();
                List<Object> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    Object item = readValueBinary(in);
                    if (item != null) {
                        list.add(item);
                    }
                }
                yield Collections.unmodifiableList(list);
            }
            default -> throw new IOException("Unknown compound binary tag: " + tag);
        };
    }

    private static final class PutOperation implements VariableOperation<Map<String, Object>> {
        @Override
        public String getId() {
            return "PUT";
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> currentValue, OperationArgs args) {
            String key = args.getString("key")
                    .orElseThrow(() -> new IllegalArgumentException("Missing 'key' argument for PUT operation"));
            Object value = args.get("value").orElse(null);

            Map<String, Object> copy = new LinkedHashMap<>(currentValue != null ? currentValue : Map.of());
            if (value == null) {
                copy.remove(key);
            } else {
                copy.put(key, normalizeValue(value));
            }
            return Collections.unmodifiableMap(copy);
        }
    }

    private static final class RemoveOperation implements VariableOperation<Map<String, Object>> {
        @Override
        public String getId() {
            return "REMOVE";
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> currentValue, OperationArgs args) {
            String key = args.getString("key")
                    .orElseThrow(() -> new IllegalArgumentException("Missing 'key' argument for REMOVE operation"));
            Map<String, Object> copy = new LinkedHashMap<>(currentValue != null ? currentValue : Map.of());
            copy.remove(key);
            return Collections.unmodifiableMap(copy);
        }
    }

    private static final class ClearOperation implements VariableOperation<Map<String, Object>> {
        @Override
        public String getId() {
            return "CLEAR";
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> currentValue, OperationArgs args) {
            return Map.of();
        }
    }
}
