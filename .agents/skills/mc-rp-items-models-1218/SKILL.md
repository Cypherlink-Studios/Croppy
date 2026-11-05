---
name: mc-rp-items-models-1218
description: Authors and manages modern item definitions (assets/<ns>/items/*.json), 3D block/item models with continuous single-axis rotation (-45° to +45°), and equipment layers for Minecraft Java 1.21.8. Use when adding custom items, custom model data, 3D weapons/tools/blocks, or custom equipment layers.
version: "1.0"
---

# Minecraft 1.21.8 Item Definitions, 3D Models & Equipment

## 1. Scope & Objective
Guide the implementation of item models, data component dispatch trees, 3D geometry constraints, and equipment layers using the modern resource pack architecture introduced in 1.21.4 and refined in 1.21.6/1.21.8.

---

## 2. Inviolable Constraints

1. **NO 'overrides' IN models/item/:** The legacy `overrides` array inside `assets/<namespace>/models/item/<item>.json` was deprecated in 1.21.4. All item dispatch logic MUST live in `assets/<namespace>/items/<item_id>.json`.
2. **ROTATION CONSTRAINTS (1.21.6 - 1.21.8):**
   - Inside `models/block/` and `models/item/`, an element's `rotation.angle` accepts **any continuous integer or floating-point value** between `-45.0` and `45.0` degrees (the 22.5° step restriction was removed in 1.21.6).
   - Rotation MUST be single-axis (`"axis": "x" | "y" | "z"`). Simultaneous multi-axis rotation (`[x, y, z]`) is NOT supported in 1.21.8 (introduced later in 26.x).
3. **CASE-SENSITIVE LOWERCASE NAMESPACES:** All item IDs, model paths, and equipment identifiers MUST be lowercase `[a-z0-9_.-]+`.

---

## 3. Modern Item Architecture (`assets/<namespace>/items/<item>.json`)

The root object of an item definition in 1.21.8 supports:

- **`oversized_in_gui`** (boolean, added in 1.21.6): When `true`, prevents models larger than 16x16 pixels from being clipped inside the GUI slot boundary. Essential for large 3D tools, culinary stations, and weapon previews.
- **`hand_animation_on_swap`** (boolean, default: `true`): Controls whether the hand lowering/raising animation triggers when changing to/from this item.
- **`model`** (dispatch object): Determines which geometry renders based on item state.

### Dispatch Model Types:

| Type | Purpose | Key Parameters |
|---|---|---|
| `minecraft:model` | Renders a direct 3D model geometry from `models/`. | `"model": "<namespace>:<path>"`, optional `"tints": [...]` |
| `minecraft:select` | Discrete string matching (enums, states, component tags). | `"property"`, `"cases": [{"when": ..., "model": ...}]`, `"fallback"` |
| `minecraft:range_dispatch` | Numerical thresholds (floats, damage, cooldown, time). | `"property"`, `"entries": [{"threshold": ..., "model": ...}]`, `"fallback"` |
| `minecraft:condition` | Boolean branch evaluation. | `"property"`, `"on_true": {...}`, `"on_false": {...}` |
| `minecraft:composite` | Layered rendering of multiple models simultaneously. | `"models": [{...}, {...}]` |
| `minecraft:special` | Hardcoded entity-like renderers (shields, heads, tridents). | `"model": {"type": "minecraft:shield"}` *(Note: head no longer loads player skin from profile)* |

---

## 4. Custom Model Data in 1.21.8

In Minecraft 1.21.4+, the `minecraft:custom_model_data` Data Component holds four independent lists: `floats`, `flags` (booleans), `strings`, and `colors`.

### String Select Dispatch (Recommended for clean namespaces):
```json
{
  "oversized_in_gui": true,
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "cases": [
      {
        "when": "croppy:recipe_book",
        "model": {
          "type": "minecraft:model",
          "model": "croppy:item/recipe_book"
        }
      },
      {
        "when": "croppy:copper_can",
        "model": {
          "type": "minecraft:model",
          "model": "croppy:item/copper_can"
        }
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/knowledge_book"
    }
  }
}
```

### Numerical Float Range Dispatch:
```json
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "entries": [
      {
        "threshold": 1001.0,
        "model": {
          "type": "minecraft:model",
          "model": "croppy:item/watering_can_rusty"
        }
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/iron_hoe"
    }
  }
}
```

---

## 5. Equipment Layer Configuration (`assets/<namespace>/equipment/<id>.json`)

Defines textures for wearable items, armor, and entity saddles/harnesses:

- Layer Types: `humanoid`, `humanoid_leggings`, `wings`, `horse_body`, `llama_body`, `camel_body`, `wolf_body`, and `happy_ghast_body` (1.21.6+).
- Texture Location: `assets/<namespace>/textures/entity/equipment/<layer_type>/<id>.png`.

```json
{
  "layers": {
    "humanoid": [
      {
        "texture": "croppy:chef_hat",
        "dyeable": { "color_when_undyed": -1 }
      }
    ]
  }
}
```

For detailed property references, consult `references/item_dispatch_properties.md`.
