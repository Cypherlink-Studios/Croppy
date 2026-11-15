---
title: Typed Variable System
description: Developer reference for DataType, SimpleDataType, CompoundDataType, DataTypeRegistry, TypedVariableStore, VariableOperation, and resilient binary codecs.
sidebar:
  order: 3
---

# Typed Variable System (`dev.darkblade.croppy.api.variable`)

Croppy provides an extensible, type-safe variable engine allowing workstations (and future domain entities) to store custom runtime variables under the `var.*` namespace, validate YAML defaults, apply strictly typed mutations, and persist state safely to SQLite and PDC even if an addon is temporarily uninstalled.

> For configuring `default_variables:` in station YAML files, see [Workstations Configuration](../workstations-configuration.md#typed-custom-variables-default_variables).

---

## 1. Built-in Data Types (`DataTypeRegistry`)

`DataTypeRegistry` (`Registry<DataType<?>>, Service`) manages all registered data types by canonical `Key`:

| Key | Java Value Type | Built-in `VariableOperation` IDs | Notes |
|---|---|---|---|
| `croppy:integer` | `Long` | `SET`, `ADD`, `SUBTRACT`, `MULTIPLY`, `CLAMP` | Shorthand in YAML: `integer` |
| `croppy:decimal` | `Double` | `SET`, `ADD`, `SUBTRACT`, `MULTIPLY`, `CLAMP` | Shorthand in YAML: `decimal` |
| `croppy:boolean` | `Boolean` | `SET`, `TOGGLE` | Shorthand in YAML: `boolean` |
| `croppy:string` | `String` | `SET`, `APPEND` | Shorthand in YAML: `string` |
| `croppy:compound` | `Map<String, Object>` | `SET` | Shorthand in YAML: `compound`; exports nested keys via dot notation (`var.<name>.<field>`) |

Use `DataTypeRegistry#getTyped(Key key, Class<T> valueClass)` to safely resolve a `DataType<T>` with compile-time type alignment.

---

## 2. Implementing Custom `DataType<T>` Contracts

Every type implements `DataType<T>` (via `SimpleDataType<T>` for atomic values or `CompoundDataType<T>` for structured/indexed objects):

```java
public interface DataType<T> extends Keyed {
    Key getKey();
    Class<T> getValueClass();
    T getDefaultValue();
    boolean isCompound();
    T parse(Object rawInput);
    void writeBinary(DataOutput out, T value) throws IOException;
    T readBinary(DataInput in) throws IOException;
    void exportVariables(String variableName, T value, Map<String, Object> destination);
    Collection<VariableOperation<T>> getSupportedOperations();
}
```

### `SimpleDataType<T>` vs `CompoundDataType<T>`

* **`SimpleDataType<T>`**:
  * `isCompound()` defaults to `false`.
  * Override `formatValue(T value)` to customize formatted output; `exportVariables(...)` automatically exports both `<name>` and `<name>_formatted`.
* **`CompoundDataType<T>`**:
  * `isCompound()` defaults to `true`.
  * Adds `Optional<Object> getProperty(T value, String propertyPath)` for resolving nested properties or indices (e.g., `"fluid"`, `"level"`, `"0"`).
  * `exportVariables(...)` should populate dot-notated keys (e.g., `"tank.fluid"`, `"tank.level"`).

---

## 3. Type-Safe Mutations (`VariableOperation<T>` & `OperationArgs`)

Mutations are modeled as explicit, self-validating operations declared by each `DataType`:

```java
public interface VariableOperation<T> {
    String getId(); // Uppercase identifier, e.g. "SET", "ADD", "DRAIN"
    T apply(T currentValue, OperationArgs args);
}
```

`OperationArgs` wraps an immutable parameter map with typed accessors:
* Factories: `OperationArgs.EMPTY`, `OperationArgs.of(Map<String, Object>)`, `OperationArgs.of(String key, Object value)`.
* Accessors: `get(key)`, `getRequired(key)`, `getString(key)`, `getLong(key)`, `getDouble(key)`, `getBoolean(key)`, `asMap()`.

Attempting an operation ID not listed in `getSupportedOperations()` fails fast with `UnsupportedOperationException` without mutating state.

---

## 4. Runtime Storage (`TypedVariableStore` & `TypedVariable<T>`)

`TypedVariableStore` extends `VariableProvider` (defaulting `getVariableNamespace()` to `"var"`):

* **Define or update**: `<T> TypedVariable<T> define(String name, DataType<T> type, T initialValue)`
* **Type-safe lookup**: `getTypedVariable(String name, Class<T> valueClass)` returns `Optional<TypedVariable<T>>`.
* **Mutate by operation**: `Object applyOperation(String name, String operationId, OperationArgs args)` applies the named operation and marks the variable dirty.
* **Persistence tracking**: `isDirty()` and `clearDirty()` let storage backends flush only modified stores.

---

## 5. Uninstall-Safe Binary Persistence (`UnresolvedVariableData`)

When `VariableStoreBinaryCodec` deserializes a `TypedVariableStore` from SQLite or PDC and encounters a `Key` whose `DataType` is not currently registered (for example, an addon JAR was temporarily removed):

1. The raw byte payload is stored verbatim inside `UnresolvedVariableData(String name, Key typeKey, byte[] rawPayload)` via `TypedVariableStore#setUnresolvedVariable`.
2. Chunk and station loading completes without throwing exceptions or losing adjacent station state.
3. On subsequent saves, `getUnresolvedVariables()` writes the preserved bytes back untouched until the owning addon and its `DataType` return.
