---
title: Rules & Station Interactions
description: Technical specification and YAML reference for station interaction triggers and dynamic cooking failure rules with Pratt AST expression evaluation.
sidebar:
  order: 16
---

# Rules & Station Interactions

Croppy provides two event-driven declarative frameworks for station automation and failure mechanics:
1. **Station Interactions (`plugins/Croppy/interactions/*.yml`)**: Dispatches customizable player actions based on click triggers, held items, and station states.
2. **Dynamic Cooking Rules (`plugins/Croppy/rules/*.yml`)**: Evaluates real-time conditions using zero-allocation compiled **Pratt AST expressions** to trigger burnt dishes, smoke particles, and boiling sounds.

---

## 1. Station Interactions (`interactions/*.yml`)

Interactions bind player input events (such as right-clicking with a culinary thermometer or sneak-clicking a cooking pot) to discrete actions.

### Complete Interaction YAML Example
Here is the configuration for opening the cooking pot menu via `plugins/Croppy/interactions/cooking_pot_interactions.yml`:

```yaml
id: "croppy:cooking_pot_gui"

# Station key this interaction binds to
station: "croppy:cooking_pot"

# Click trigger event
trigger: "SNEAK_RIGHT_CLICK"

# Execution priority (higher priority executes first)
priority: 100

# Optional conditions evaluated before dispatching actions
conditions:
  - "station.is_heated == true"

# Action pipeline
actions:
  - type: "OPEN_GUI"
  - type: "PLAY_SOUND"
    sound: "BLOCK_CHEST_OPEN"
    pitch: 1.2
```

### Hand Matching Filters
Interactions can require specific items or empty hands:
```yaml
id: "croppy:thermometer_inspect"
station: "croppy:cooking_pot"
trigger: "RIGHT_CLICK"

# Matches when holding the culinary thermometer
match_item: "croppy:thermometer"

priority: 200

actions:
  - type: "SHOW_TEMPERATURE"
  - type: "PLAY_SOUND"
    sound: "BLOCK_NOTE_BLOCK_BELL"
    pitch: 1.5
```

### Interaction Triggers
* `RIGHT_CLICK`: Standard hand interaction on the station block.
* `SNEAK_RIGHT_CLICK`: Shift-click interaction on the station block.
* `LEFT_CLICK`: Punching or striking the station.
* `SNEAK_LEFT_CLICK`: Shift-striking the station.

---

## 2. Dynamic Cooking Rules (`rules/*.yml`)

Rules run continuously during the server's station simulation loop (`STATION_TICK`). When conditions evaluate to `true`, the rule fires configured actions to enforce overcooking penalties, smoke emissions, or recipe destruction.

### Complete Rule YAML Example
Here is the overcooking penalty configuration from `plugins/Croppy/rules/overcooking_burn.yml`:

```yaml
id: "croppy:overcooking_burn"
name: "Overcooking Burn"

# Evaluation trigger lifecycle
trigger: "STATION_TICK"

# Rule severity classification: INFO, WARNING, or CRITICAL
severity: "CRITICAL"

# Priority order
priority: 100

# Pre-compiled Pratt AST boolean condition expressions
conditions:
  - "process.finished == true"
  - "process.idle_ticks > 200"
  - "station.temperature > 120"

# Failure and feedback actions
actions:
  - type: "REPLACE_OUTPUT"
    target_item: "croppy:burnt_food"
  - type: "EMIT_PARTICLES"
    particle: "LARGE_SMOKE"
    count: 8
  - type: "PLAY_SOUND"
    sound: "ENTITY_GENERIC_BURN"
```

---

## 3. Pratt AST Condition Expression Grammar

Conditions in Croppy rules are compiled into an Abstract Syntax Tree (AST) at server startup via `ExpressionCompiler`. This guarantees **zero heap allocations** during high-frequency server tick cycles.

```text
+-----------------------------------------------------------------------------+
|                      ZERO-ALLOCATION AST CONDITION PIPELINE                 |
+-----------------------------------------------------------------------------+
|                                                                             |
|  "process.finished == true and station.temperature > 120"                   |
|                                |                                            |
|                                v  (Compiled by YamlRuleLoader on startup)   |
|                    [Binary Operator: AND]                                   |
|                    /                    \                                   |
|       [Comparison: ==]                 [Comparison: >]                      |
|       /              \                 /             \                      |
|  [process.finished] [true]      [station.temperature] [120]                 |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### Supported Operators
* **Equality & Comparison**: `==`, `!=`, `<`, `<=`, `>`, `>=`
* **Logical Conjunctions**: `and`, `&&`, `or`, `||`, `not`, `!`
* **Grouping**: Parentheses `( ... )` for controlling operator precedence

### Contextual Variables

| Variable | Type | Description |
|---|---|---|
| `process.finished` | Boolean | `true` when cooking progress reaches 100%. |
| `process.progress` | Number | Active cooking progress from `0.0` to `1.0`. |
| `process.idle_ticks` | Number | Ticks elapsed since the dish reached completion without being removed. |
| `station.temperature` | Number | Current temperature in Celsius. |
| `station.is_heated` | Boolean | `true` if current temperature is above ambient room temperature (>25°C). |
| `station.empty` | Boolean | `true` if the station has zero ingredients and zero outputs. |

---

## 4. Rule Actions Reference

| Action Type | Key Parameters | Description |
|---|---|---|
| `REPLACE_OUTPUT` | `target_item: String` | Replaces all items in the station's output slots with another item (e.g., `croppy:burnt_food`). |
| `CANCEL_PROCESS` | — | Immediately resets cooking progress back to 0%. |
| `EMIT_PARTICLES` | `particle: String`, `count: Int` | Emits particle effects at the station's block center (e.g. `LARGE_SMOKE`, `FLAME`, `CAMPFIRE_COSY_SMOKE`). |
| `PLAY_SOUND` | `sound: String`, `pitch: Float`, `volume: Float` | Plays a directional sound effect for nearby players (e.g. `ENTITY_GENERIC_BURN`, `BLOCK_FIRE_EXTINGUISH`). |
| `DROP_INPUTS` | — | Ejects all simmering ingredients into the world as dropped item entities. |
