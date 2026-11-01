---
title: Custom Items & Tools
description: Catalog of Croppy's custom items, agricultural tools, PDC data persistence, and item registration format.
sidebar:
  order: 14
---

# Custom Items & Agricultural Tools

Croppy provides a unified item framework that bridges vanilla items with custom behavioral items. All items are identified by semantic keys (`croppy:<item_name>`), support CustomModelData for texture packs, and embed durable metadata into Minecraft's native **PersistentDataContainer (PDC)**.

---

## 1. Agricultural Tools Catalog

Croppy equips farmers and chefs with specialized tools:

```text
+-----------------------------------------------------------------------------+
|                          AGRICULTURAL & CHEF TOOLS                          |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [SOIL & CROP INSPECTOR] (croppy:inspector)                                 |
|  - Base Material: SPYGLASS                                                  |
|  - Function: Right-click any farmland or growing crop to display its live   |
|    inspection report (species, stage, progress %, light, moisture, NPK).    |
|                                                                             |
|  [CULINARY THERMOMETER] (croppy:thermometer)                                |
|  - Base Material: BLAZE_ROD                                                 |
|  - Function: Right-click any cooking station or stove to read its exact     |
|    surface temperature in Celsius and active thermal zone.                  |
|                                                                             |
|  [RECIPE BOOK] (croppy:recipe_book)                                         |
|  - Base Material: BOOK                                                      |
|  - Function: Right-click in hand to instantly open the interactive         |
|    Culinary Compendium cookbook GUI.                                        |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Item Categories

Items are organized under semantic categories inside `plugins/Croppy/items/<category>/`:

| Category | Typical Contents | Examples |
|---|---|---|
| `seeds` | Plantable crop seeds linking to species definitions | `croppy:wheat_seeds`, `croppy:carrot_seeds` |
| `tools` | Diagnostic and operational implements | `croppy:inspector`, `croppy:thermometer` |
| `watering_cans` | Hydration equipment with capacities and AoE widths | `croppy:copper_can`, `croppy:gold_can` |
| `fertilizers` | Nutrient enhancements (Speed, Quality, Retaining) | `croppy:speed_gro`, `croppy:retaining_soil` |
| `stations` | Placeable physical block workstation items | `croppy:cooking_pot`, `croppy:skillet`, `croppy:stove` |
| `misc` | Encyclopedias, cooking manuals, and general items | `croppy:recipe_book` |

---

## 3. Persistent Data Container (PDC) Architecture

Every Croppy item embeds persistent tags within its PDC:
* **`croppy:item_id`**: The immutable semantic identifier linking the physical item to its registry definition.
* **`croppy:water_level` / `croppy:capacity`**: Tracks remaining water charges on watering cans.
* **`croppy:fertilizer_type` / `croppy:bonus`**: Tracks applied fertilizer properties on seed packets or enriched soil.

Because data is stored natively inside Minecraft's NBT/PDC structures, items survive server restarts, player inventory changes, and container movements without desynchronization.
