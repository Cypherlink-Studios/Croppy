---
title: Variable Providers & Expressions
description: Developer reference for VariableProvider, EvaluationContext, Expression, VariableResolver, component namespaces, and Pratt AST evaluation.
sidebar:
  order: 4
---

# Variable Providers & Expressions (`dev.darkblade.croppy.api.expression`)

Croppy exposes runtime component state, typed custom variables, and display telemetry through a unified, zero-allocation expression and context pipeline shared by station states (`when:`), dynamic rules (`conditions:`), interactions, and floating text holograms.

> For YAML configuration of station states and dynamic rules, see [Workstations Configuration](../workstations-configuration.md) and [Rules & Station Interactions](../rules-and-interactions.md).

---

## 1. `VariableProvider` Contract

Any `StationComponent` (or addon component) can implement `dev.darkblade.croppy.api.expression.VariableProvider` to contribute variables under a dedicated namespace:

```java
public interface VariableProvider {
    String getVariableNamespace();
    void populateVariables(Map<String, Object> variables);

    default void exportNamespaced(Map<String, Object> target) { ... }
}
```

* **`getVariableNamespace()`**: Returns a unique non-blank prefix such as `"thermal"`, `"heat"`, `"storage"`, `"process"`, `"display"`, `"var"`, or a custom addon namespace like `"alembic"`.
* **`populateVariables(Map<String, Object> variables)`**: Populates relative keys (e.g., `"pressure"`, `"tank.fluid"`) with raw values for AST comparisons and formatted strings for holograms.
* **`exportNamespaced(target)`**: Automatically prefixes non-null entries with `<namespace>.` before inserting into `target`.

---

## 2. `EvaluationContext`, `Expression`, and `VariableResolver`

### `EvaluationContext`

`EvaluationContext` is an immutable evaluation scope built per tick or interaction:

* **Factories**: `EvaluationContext.empty()` and `EvaluationContext.of(Map<String, Object>)`.
* **Enrichment**: `with(key, value)`, `withAll(map)`, `withProvider(VariableProvider)`, and `withResolver(VariableResolver)`.
* **Lookup**: `Optional<Object> getVariable(String name)` checks local variable bindings first, then iterates registered `VariableResolver`s in order.

### `Expression` & `VariableResolver`

* **`Expression`** (`@FunctionalInterface`): `boolean evaluate(EvaluationContext context)` (plus `Expression.ALWAYS_TRUE` and `Expression.ALWAYS_FALSE`). Compiled at startup by `ExpressionCompiler` into a Pratt AST so recurring station ticks incur zero heap allocations.
* **`VariableResolver`** (`@FunctionalInterface`): `Optional<Object> resolve(String name, EvaluationContext context)` for lazy or dynamically computed variables.

---

## 3. Standard Component Namespaces

During station evaluation, built-in components implement `VariableProvider` and export the following keys:

| Namespace (`Provider`) | Exported Keys & Types |
|---|---|
| `thermal.*` (`ThermalComponent`) | `thermal.temp` (`Double`), `thermal.temperature` (`Double`), `thermal.temp_c` (`String`, e.g. `100.0°C`), `thermal.target_temp` (`Double`), `thermal.zone` (`String`: `COLD`..`BURNING`), `thermal.is_heated` (`Boolean`) |
| `heat.*` (`HeatSourceComponent`) | `heat.fuel_ticks` (`Long`), `heat.max_fuel_ticks` (`Long`), `heat.is_active` (`Boolean`), `heat.is_burning` (`Boolean`), `heat.target_temp` (`Double`), `heat.output_temp` (`Double`), `heat.output_temp_c` (`String`), `heat.min_temp` (`Double`), `heat.max_temp` (`Double`) |
| `storage.*` (`StorageComponent`) | `storage.has_inputs` (`Boolean`), `storage.input_count` (`Integer`), `storage.input_amount` (`Integer`), `storage.has_outputs` (`Boolean`), `storage.output_count` (`Integer`), `storage.output_amount` (`Integer`), `storage.is_empty` (`Boolean`), `storage.input_capacity` (`Integer`), `storage.output_capacity` (`Integer`) |
| `process.*` (`ProcessComponent`) | `process.has_process` (`Boolean`), `process.state` (`String`: `IDLE`, `RUNNING`, `AWAITING_INPUT`, `COMPLETED`, `PAUSED`), `process.finished` (`Boolean`), `process.idle_ticks` (`Long`), `process.key` (`String`), `process.recipe_name` (`String`), `process.phase_index` (`Integer`), `process.phase_elapsed` (`Double`), `process.total_elapsed` (`Double`), `process.progress` (`Double` `0.0–1.0`), `process.progress_percent` (`Integer`), `process.progress_bar` (`String`) |
| `display.*` (`DisplayComponent`) | `display.status_text` (`String`), `display.progress` (`Double`), `display.progress_percent` (`Integer`), `display.thermal_zone` (`String`), plus `display.headroom` (`Double`, available during `DISPLAY_LAYOUT` evaluation) |
| `var.*` (`TypedVariableStore`) | Custom simple values (`var.<name>`, `var.<name>_formatted`) and compound fields (`var.<name>.<field>`) |

> [!NOTE]
> Un-namespaced legacy aliases (`temperature`, `temp`, `thermal_zone`, `fuel_ticks`, `is_active`, `process_state`, `has_inputs`, `input_count`, `has_outputs`, `station.temperature`, `station.is_heated`, `station.thermal_zone`, `station.empty`) remain supported for backward compatibility, though new configurations should prefer the explicit `thermal.*`, `heat.*`, `storage.*`, and `process.*` namespaces.

---

## 4. Pratt AST Grammar & `ContextualTextFormatter`

```text
+-----------------------------------------------------------------------------+
|                      ZERO-ALLOCATION AST CONDITION PIPELINE                 |
+-----------------------------------------------------------------------------+
|                                                                             |
|  "process.finished == true and thermal.temp > 120"                          |
|                                |                                            |
|                                v  (Compiled at startup via ExpressionCompiler)|
|                    [Binary Operator: AND]                                   |
|                    /                    \                                   |
|       [Comparison: ==]                 [Comparison: >]                      |
|       /              \                 /             \                      |
|  [process.finished] [true]      [thermal.temp]      [120]                   |
|                                                                             |
+-----------------------------------------------------------------------------+
```

* **Comparison operators**: `==`, `!=`, `<`, `<=`, `>`, `>=`
* **Logical operators**: `and`, `&&`, `or`, `||`, `not`, `!`
* **Grouping**: `( ... )`

In virtual display templates, `ContextualTextFormatter` resolves any `{key}` token in a single pass directly against the station's unified `EvaluationContext` (supporting `{thermal.temp_c}`, `{var.cleanliness}`, `{var.tank.fluid}`, and standard cooking display placeholders).
