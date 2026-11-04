---
title: Configurable Menu System
description: Guide to 9xN pattern-based inventory menus, input/output slot mappings, dynamic status states, and button actions.
sidebar:
  order: 17
---

# Configurable Menu System

Croppy features a flexible, declarative inventory menu engine located in `plugins/Croppy/menus/`. Server administrators can customize the entire layout of cooking stations, fuel fireboxes, and compendium interfaces using visual ASCII character matrices.

---

## 1. Matrix Layout & Pattern Definition

Menus define an overall dimension (`rows: 1..6`) and a row pattern where each character symbol corresponds to a functional element:

```yaml
id: "croppy:cooking_pot"
title: "<gradient:#ffaa00:#ff5555>Cooking Pot</gradient>"
rows: 4

pattern:
  - "X X X X X X X X X"
  - "X I I I . O O . X"
  - "X I I I . S . . X"
  - "X X X X B X X X X"
```

### Pattern Symbols Mapping
* **`X`**: Border decoration items (e.g. stained glass panes).
* **`I`**: Input slots where raw agricultural ingredients are inserted.
* **`O`**: Output slots where completed dishes are dispatched for collection.
* **`S`**: Dynamic status indicator reflecting live recipe progress.
* **`B`**: Action buttons triggering station interactions (e.g., ignite, start, cancel).
* **`.`**: Empty interactable slots.

---

## 2. Element Types

### A. Input & Output Slots (`INPUT` / `OUTPUT`)
Designates slots as containers for station items. The engine automatically enforces ingredient capacity limits:
```yaml
elements:
  'I':
    type: INPUT
  'O':
    type: OUTPUT
```

### B. Dynamic Status Elements (`STATUS`)
Status elements change their icon, name, and lore in real time based on boolean evaluation expressions:
```yaml
elements:
  'S':
    type: STATUS
    item:
      material: "minecraft:cauldron"
      name: "<gray>Cooking Pot: <italic>Idle</italic></gray>"
    states:
      - condition: "process.state == 'RUNNING'"
        item:
          material: "minecraft:campfire"
          name: "<gold><bold>Cooking...</bold></gold>"
          lore:
            - "<yellow>Recipe:</yellow> <white>{recipe.name}</white>"
            - "<yellow>Progress:</yellow> <aqua>{process.progress_percent}%</aqua>"
            - "<yellow>Heat:</yellow> <white>{station.heat_level}°C</white>"
          glow: true

      - condition: "process.state == 'STALLED'"
        item:
          material: "minecraft:barrier"
          name: "<red><bold>Stalled: Insufficient Heat!</bold></red>"
```

### C. Interactive Action Buttons (`ACTION`)
Buttons respond to specific click filters (`LEFT`, `RIGHT`, `SHIFT_LEFT`) to invoke engine operations:
```yaml
elements:
  'B':
    type: ACTION
    item:
      material: "minecraft:flint_and_steel"
      name: "<yellow><bold>Ignite Cookware</bold></yellow>"
    actions:
      - click: LEFT
        type: STATION_ACTION
        action: "START_PROCESS"
```
