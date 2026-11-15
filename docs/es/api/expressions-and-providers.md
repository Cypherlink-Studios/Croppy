---
title: Proveedores de Variables y Expresiones
description: Referencia técnica para VariableProvider, EvaluationContext, Expression, VariableResolver, namespaces de componentes y evaluación AST Pratt.
sidebar:
  order: 4
---

# Proveedores de Variables y Expresiones (`dev.darkblade.croppy.api.expression`)

Croppy expone el estado en tiempo de ejecución de los componentes, variables tipadas personalizadas y telemetría de display mediante una tubería unificada de cero asignaciones compartida por estados de estación (`when:`), reglas dinámicas (`conditions:`), interacciones y hologramas flotantes.

> Para configurar estados de estaciones y reglas dinámicas en YAML, consulta [Configuración de Estaciones de Trabajo](../workstations-configuration.md) y [Reglas e Interacciones de Estaciones](../rules-and-interactions.md).

---

## 1. Contrato `VariableProvider`

Cualquier `StationComponent` (o componente de un addon) puede implementar `dev.darkblade.croppy.api.expression.VariableProvider` para aportar variables bajo un namespace dedicado:

```java
public interface VariableProvider {
    String getVariableNamespace();
    void populateVariables(Map<String, Object> variables);

    default void exportNamespaced(Map<String, Object> target) { ... }
}
```

* **`getVariableNamespace()`**: Devuelve un prefijo único no vacío como `"thermal"`, `"heat"`, `"storage"`, `"process"`, `"display"`, `"var"` o un namespace propio de addon como `"alembic"`.
* **`populateVariables(Map<String, Object> variables)`**: Rellena claves relativas (ej. `"pressure"`, `"tank.fluid"`) con valores crudos para comparaciones AST y cadenas formateadas para hologramas.
* **`exportNamespaced(target)`**: Antepone automáticamente `<namespace>.` a las entradas no nulas al insertarlas en `target`.

---

## 2. `EvaluationContext`, `Expression` y `VariableResolver`

### `EvaluationContext`

`EvaluationContext` es un ámbito de evaluación inmutable construido por cada tick o interacción:

* **Fábricas**: `EvaluationContext.empty()` y `EvaluationContext.of(Map<String, Object>)`.
* **Enriquecimiento**: `with(key, value)`, `withAll(map)`, `withProvider(VariableProvider)` y `withResolver(VariableResolver)`.
* **Consulta**: `Optional<Object> getVariable(String name)` revisa primero las variables locales y luego recorre los `VariableResolver` registrados en orden.

### `Expression` y `VariableResolver`

* **`Expression`** (`@FunctionalInterface`): `boolean evaluate(EvaluationContext context)` (además de `Expression.ALWAYS_TRUE` y `Expression.ALWAYS_FALSE`). Compilada en el arranque por `ExpressionCompiler` en un árbol AST Pratt para evitar asignaciones en el heap durante ticks frecuentes.
* **`VariableResolver`** (`@FunctionalInterface`): `Optional<Object> resolve(String name, EvaluationContext context)` para variables perezosas o calculadas dinámicamente.

---

## 3. Namespaces Estándar de Componentes

Durante la evaluación de estaciones, los componentes integrados implementan `VariableProvider` y exportan las siguientes claves:

| Namespace (`Provider`) | Claves Exportadas y Tipos |
|---|---|
| `thermal.*` (`ThermalComponent`) | `thermal.temp` (`Double`), `thermal.temperature` (`Double`), `thermal.temp_c` (`String`, ej. `100.0°C`), `thermal.target_temp` (`Double`), `thermal.zone` (`String`: `COLD`..`BURNING`), `thermal.is_heated` (`Boolean`) |
| `heat.*` (`HeatSourceComponent`) | `heat.fuel_ticks` (`Long`), `heat.max_fuel_ticks` (`Long`), `heat.is_active` (`Boolean`), `heat.is_burning` (`Boolean`), `heat.target_temp` (`Double`), `heat.output_temp` (`Double`), `heat.output_temp_c` (`String`), `heat.min_temp` (`Double`), `heat.max_temp` (`Double`) |
| `storage.*` (`StorageComponent`) | `storage.has_inputs` (`Boolean`), `storage.input_count` (`Integer`), `storage.input_amount` (`Integer`), `storage.has_outputs` (`Boolean`), `storage.output_count` (`Integer`), `storage.output_amount` (`Integer`), `storage.is_empty` (`Boolean`), `storage.input_capacity` (`Integer`), `storage.output_capacity` (`Integer`) |
| `process.*` (`ProcessComponent`) | `process.has_process` (`Boolean`), `process.state` (`String`: `IDLE`, `RUNNING`, `AWAITING_INPUT`, `COMPLETED`, `PAUSED`), `process.finished` (`Boolean`), `process.idle_ticks` (`Long`), `process.key` (`String`), `process.recipe_name` (`String`), `process.phase_index` (`Integer`), `process.phase_elapsed` (`Double`), `process.total_elapsed` (`Double`), `process.progress` (`Double` `0.0–1.0`), `process.progress_percent` (`Integer`), `process.progress_bar` (`String`) |
| `display.*` (`DisplayComponent`) | `display.status_text` (`String`), `display.progress` (`Double`), `display.progress_percent` (`Integer`), `display.thermal_zone` (`String`), además de `display.headroom` (`Double`, disponible en evaluaciones `DISPLAY_LAYOUT`) |
| `var.*` (`TypedVariableStore`) | Valores simples (`var.<nombre>`, `var.<nombre>_formatted`) y campos compuestos (`var.<nombre>.<campo>`) |

> [!NOTE]
> Los alias heredados sin namespace (`temperature`, `temp`, `thermal_zone`, `fuel_ticks`, `is_active`, `process_state`, `has_inputs`, `input_count`, `has_outputs`, `station.temperature`, `station.is_heated`, `station.thermal_zone`, `station.empty`) siguen soportados por compatibilidad hacia atrás, pero las nuevas configuraciones deben preferir `thermal.*`, `heat.*`, `storage.*` y `process.*`.

---

## 4. Gramática AST Pratt y `ContextualTextFormatter`

```text
+-----------------------------------------------------------------------------+
|                    PIPELINE AST DE CERO ASIGNACIONES                        |
+-----------------------------------------------------------------------------+
|                                                                             |
|  "process.finished == true and thermal.temp > 120"                          |
|                                |                                            |
|                                v  (Compilado al inicio con ExpressionCompiler)|
|                    [Operador Binario: AND]                                  |
|                    /                    \                                   |
|       [Comparación: ==]                [Comparación: >]                     |
|       /              \                 /             \                      |
|  [process.finished] [true]      [thermal.temp]      [120]                   |
|                                                                             |
+-----------------------------------------------------------------------------+
```

* **Operadores de comparación**: `==`, `!=`, `<`, `<=`, `>`, `>=`
* **Operadores lógicos**: `and`, `&&`, `or`, `||`, `not`, `!`
* **Agrupación**: `( ... )`

En plantillas de displays virtuales, `ContextualTextFormatter` resuelve cualquier token `{clave}` en una sola pasada directamente contra el `EvaluationContext` unificado de la estación (soportando `{thermal.temp_c}`, `{var.cleanliness}`, `{var.tank.fluid}` y los placeholders estándar de cocción).
