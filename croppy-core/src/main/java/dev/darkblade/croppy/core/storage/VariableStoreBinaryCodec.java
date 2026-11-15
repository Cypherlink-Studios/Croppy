package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.variable.DataType;
import dev.darkblade.croppy.api.variable.DataTypeRegistry;
import dev.darkblade.croppy.api.variable.TypedVariable;
import dev.darkblade.croppy.api.variable.TypedVariableStore;
import dev.darkblade.croppy.api.variable.UnresolvedVariableData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Binary serializer/deserializer for {@link TypedVariableStore} instances.
 * Encodes each variable as {@code (name, typeKey, payloadLength, payloadBytes)},
 * preserving unresolved addon variables safely if their {@link DataType} is missing.
 */
public final class VariableStoreBinaryCodec {

    private VariableStoreBinaryCodec() {}

    /**
     * Writes all resolved and unresolved variables from {@code store} into {@code out}.
     *
     * @param out   target binary output stream
     * @param store source variable store
     * @throws IOException if an I/O error occurs
     */
    public static void write(DataOutput out, TypedVariableStore store) throws IOException {
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(store, "store cannot be null");

        Map<String, TypedVariable<?>> resolved = store.getAllVariables();
        Map<String, UnresolvedVariableData> unresolved = store.getUnresolvedVariables();

        out.writeInt(resolved.size() + unresolved.size());

        for (TypedVariable<?> var : resolved.values()) {
            out.writeUTF(var.getName());
            out.writeUTF(var.getType().getKey().asString());
            byte[] payload = encodeValue(var);
            out.writeInt(payload.length);
            out.write(payload);
        }

        for (UnresolvedVariableData data : unresolved.values()) {
            out.writeUTF(data.name());
            out.writeUTF(data.typeKey().asString());
            byte[] payload = data.rawPayload();
            out.writeInt(payload.length);
            out.write(payload);
        }
    }

    /**
     * Reads variables from {@code in} into {@code targetStore}, resolving types via {@code registry}.
     * Unknown types are preserved as unresolved variables without failing deserialization.
     *
     * @param in          source binary input stream
     * @param targetStore target variable store
     * @param registry    data type registry
     * @throws IOException if an I/O error occurs
     */
    public static void read(DataInput in, TypedVariableStore targetStore, DataTypeRegistry registry) throws IOException {
        Objects.requireNonNull(in, "in cannot be null");
        Objects.requireNonNull(targetStore, "targetStore cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");

        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String name = in.readUTF();
            Key typeKey = Key.parse(in.readUTF());
            int length = in.readInt();
            if (length < 0) {
                throw new IOException("Negative payload length for variable '" + name + "': " + length);
            }
            byte[] payload = new byte[length];
            in.readFully(payload);

            Optional<DataType<?>> typeOpt = registry.get(typeKey);
            if (typeOpt.isPresent()) {
                try {
                    decodeAndDefine(name, typeOpt.get(), payload, targetStore);
                } catch (IOException ex) {
                    // Preserve payload if type schema read fails
                    targetStore.setUnresolvedVariable(name, typeKey, payload);
                }
            } else {
                targetStore.setUnresolvedVariable(name, typeKey, payload);
            }
        }
        targetStore.clearDirty();
    }

    /**
     * Skips a serialized variable store block without parsing values.
     *
     * @param in source binary input stream
     * @throws IOException if an I/O error occurs
     */
    public static void skip(DataInput in) throws IOException {
        Objects.requireNonNull(in, "in cannot be null");
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            in.readUTF(); // name
            in.readUTF(); // typeKey
            int length = in.readInt();
            if (length > 0) {
                byte[] skipped = new byte[length];
                in.readFully(skipped);
            }
        }
    }

    private static <T> byte[] encodeValue(TypedVariable<T> var) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream tempOut = new DataOutputStream(baos)) {
            var.getType().writeBinary(tempOut, var.getValue());
        }
        return baos.toByteArray();
    }

    private static <T> void decodeAndDefine(
            String name,
            DataType<T> type,
            byte[] payload,
            TypedVariableStore targetStore
    ) throws IOException {
        try (DataInputStream tempIn = new DataInputStream(new ByteArrayInputStream(payload))) {
            T value = type.readBinary(tempIn);
            targetStore.define(name, type, value);
        }
    }
}
