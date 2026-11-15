---
title: Sistema de Variables Tipadas
description: Referencia para desarrolladores sobre DataType, SimpleDataType, CompoundDataType, DataTypeRegistry, TypedVariableStore, VariableOperation y códecs binarios resilientes.
sidebar:
  order: 3
---

# Sistema de Variables Tipadas (`dev.darkblade.croppy.api.variable`)

Croppy provee un motor extensible y tipado de variables que permite a las estaciones de trabajo (y futuras entidades del dominio) almacenar variables en tiempo de ejecución bajo el namespace `var.*`, validar valores iniciales en YAML, aplicar mutaciones estrictamente tipadas y persistir estado de forma segura en SQLite y PDC incluso si un addon es desinstalado temporalmente.

> Para configurar `default_variables:` en archivos YAML de estaciones, consulta [Configuración de Estaciones de Trabajo](../workstations-configuration.md#variables-tipadas-personalizadas-default_variables).

---

## 1. Tipos de Datos Integrados (`DataTypeRegistry`)

`DataTypeRegistry` (`Registry<DataType<?>>, Service`) administra todos los tipos de datos registrados por `Key` canónica:

| Key | Tipo Java | IDs de `VariableOperation` | Notas |
|---|---|---|---|
| `croppy:integer` | `Long` | `SET`, `ADD`, `SUBTRACT`, `MULTIPLY`, `CLAMP` | Alias en YAML: `integer` |
| `croppy:decimal` | `Double` | `SET`, `ADD`, `SUBTRACT`, `MULTIPLY`, `CLAMP` | Alias en YAML: `decimal` |
| `croppy:boolean` | `Boolean` | `SET`, `TOGGLE` | Alias en YAML: `boolean` |
| `croppy:string` | `String` | `SET`, `APPEND` | Alias en YAML: `string` |
| `croppy:compound` | `Map<String, Object>` | `SET` | Alias en YAML: `compound`; exporta claves anidadas con notación de punto (`var.<nombre>.<campo>`) |

Usa `DataTypeRegistry#getTyped(Key key, Class<T> valueClass)` para resolver de forma segura un `DataType<T>` con alineación de tipos en compilación.

---

## 2. Implementación de Contratos `DataType<T>` Personalizados

Cada tipo implementa `DataType<T>` (mediante `SimpleDataType<T>` para valores atómicos o `CompoundDataType<T>` para estructuras compuestas o indexadas):

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
  * `isCompound()` devuelve `false` por defecto.
  * Sobrescribe `formatValue(T value)` para personalizar el texto formateado; `exportVariables(...)` exporta automáticamente `<nombre>` y `<nombre>_formatted`.
* **`CompoundDataType<T>`**:
  * `isCompound()` devuelve `true` por defecto.
  * Añade `Optional<Object> getProperty(T value, String propertyPath)` para resolver propiedades anidadas o índices (ej. `"fluid"`, `"level"`, `"0"`).
  * `exportVariables(...)` debe poblar claves con notación de punto (ej. `"tank.fluid"`, `"tank.level"`).

---

## 3. Mutaciones Tipadas (`VariableOperation<T>` y `OperationArgs`)

Las mutaciones se modelan como operaciones explícitas y autovalidadas declaradas por cada `DataType`:

```java
public interface VariableOperation<T> {
    String getId(); // Identificador en mayúsculas, ej. "SET", "ADD", "DRAIN"
    T apply(T currentValue, OperationArgs args);
}
```

`OperationArgs` envuelve un mapa inmutable de parámetros con accesores tipados:
* Fábricas: `OperationArgs.EMPTY`, `OperationArgs.of(Map<String, Object>)`, `OperationArgs.of(String key, Object value)`.
* Accesores: `get(key)`, `getRequired(key)`, `getString(key)`, `getLong(key)`, `getDouble(key)`, `getBoolean(key)`, `asMap()`.

Intentar ejecutar un ID de operación no incluido en `getSupportedOperations()` falla inmediatamente con `UnsupportedOperationException` sin mutar el estado.

---

## 4. Almacenamiento en Runtime (`TypedVariableStore` y `TypedVariable<T>`)

`TypedVariableStore` extiende `VariableProvider` (con `"var"` como valor por defecto en `getVariableNamespace()`):

* **Definir o actualizar**: `<T> TypedVariable<T> define(String name, DataType<T> type, T initialValue)`
* **Consulta tipada**: `getTypedVariable(String name, Class<T> valueClass)` retorna `Optional<TypedVariable<T>>`.
* **Mutar por operación**: `Object applyOperation(String name, String operationId, OperationArgs args)` aplica la operación nombrada y marca la variable como modificada (`dirty`).
* **Seguimiento de persistencia**: `isDirty()` y `clearDirty()` permiten a los backends de almacenamiento guardar solo las instancias modificadas.

---

## 5. Persistencia Binaria Segura ante Desinstalación (`UnresolvedVariableData`)

Cuando `VariableStoreBinaryCodec` deserializa un `TypedVariableStore` desde SQLite o PDC y encuentra una `Key` cuyo `DataType` no está registrado actualmente (por ejemplo, el JAR de un addon fue retirado temporalmente):

1. El payload binario crudo se conserva intacto dentro de `UnresolvedVariableData(String name, Key typeKey, byte[] rawPayload)` mediante `TypedVariableStore#setUnresolvedVariable`.
2. La carga del chunk y de la estación finaliza sin lanzar excepciones ni perder el resto del estado de la estación.
3. En guardados posteriores, `getUnresolvedVariables()` vuelve a escribir los bytes preservados tal cual hasta que regrese el addon propietario y su `DataType`.
