---
title: Commands & Permissions
description: Complete command reference and permission node listing for Croppy's Cloud 2.0 command framework.
sidebar:
  order: 4
---

# Commands & Permissions Reference

Croppy features an intuitive command framework built on **Incendo Cloud 2.0**, offering rich tab-completion, permission filtering, and formatted player feedback.

The root command alias is `/croppy`.

---

## 1. Player Commands

| Command | Permission | Description |
|---|---|---|
| `/croppy recipes` | *None (All Players)* | Opens the interactive Culinary Compendium GUI. |
| `/croppy recipes <category>` | *None (All Players)* | Opens the compendium filtered to a specific category (`bakery`, `soups`, `cooking`, `beverages`, `preserves`, `all`). |

---

## 2. Administrative Commands (`croppy.admin`)

All administrative subcommands require the **`croppy.admin`** permission node.

### Custom Item & Tool Management

```text
/croppy item give <category> <item_id> [quantity]
/croppy item list [category]
```

* **`/croppy item give <category> <item_id> [quantity]`**:
  Spawns custom registered items directly into the executor's or target's inventory.
  * Categories: `seeds`, `watering_cans`, `fertilizers`, `tools`, `stations`, `misc`.
  * Examples:
    * `/croppy item give watering_cans copper_can 1`
    * `/croppy item give seeds wheat_seeds 16`
    * `/croppy item give tools inspector 1`
    * `/croppy item give misc recipe_book 1`
* **`/croppy item list [category]`**:
  Lists all item IDs registered in the internal item registry, optionally filtered by category.

---

### Crop & Farmland Inspection

```text
/croppy inspect
/croppy inspect <x> <y> <z>
```

* **`/croppy inspect`**:
  Raycasts up to 5 blocks from the player's crosshair and displays a detailed status card of the targeted crop and farmland.
* **`/croppy inspect <x> <y> <z>`**:
  Inspects the crop state at specific world block coordinates.

---

### Soil & Growth Simulation Debugging

```text
/croppy water
/croppy fertilize <fertilizer_id>
/croppy setstage <stage_index>
/croppy setprogress <progress_percent>
```

* **`/croppy water`**:
  Immediately hydrates the targeted farmland block to maximum moisture (`8/8`).
* **`/croppy fertilize <fertilizer_id>`**:
  Applies the specified fertilizer (e.g., `croppy:speed_gro`, `croppy:quality_fertilizer`) directly to the targeted soil block, modifying its N-P-K nutrient reserves.
* **`/croppy setstage <stage_index>`**:
  Forces the targeted crop to a specific lifecycle stage index (0 to max stage for that species), instantly updating block states or visual models.
* **`/croppy setprogress <progress_percent>`**:
  Sets the crop's internal growth progress percentage (0.0 to 100.0).

---

### Recipe Knowledge Management

```text
/croppy recipe list
/croppy recipe give <player> <recipe_id>
/croppy recipe revoke <player> <recipe_id>
```

* **`/croppy recipe list`**:
  Displays all loaded recipes registered in the `RecipeRegistry`.
* **`/croppy recipe give <player> <recipe_id>`**:
  Directly unlocks a recipe in the player's knowledge storage (`UNLOCKED` state).
* **`/croppy recipe revoke <player> <recipe_id>`**:
  Resets a player's knowledge for the given recipe back to `LOCKED`.

---

### Plugin Lifecycle & Reload

```text
/croppy reload
```

* **`/croppy reload`**:
  Safely reloads `config.yml`, species definitions (`crops/`), station definitions (`stations/`), item definitions, menu templates, and recipe YAML files without requiring a server reboot.

---

## 3. Permission Nodes Summary

| Permission Node | Default | Description |
|---|---|---|
| `croppy.admin` | `op` | Full access to all `/croppy` administrative and debugging subcommands. |
