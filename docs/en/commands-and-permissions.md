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

### Interactive Item Catalog GUI & Spawner

```text
/croppy items [category | search | clear | next | prev] [query]
```

* **`/croppy items`**:
  Opens the interactive Item Catalog GUI showing all registered items across all categories.
* **`/croppy items <category>`**:
  Opens the catalog directly filtered to a specific category tab (`all`, `seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`).
* **`/croppy items search [query]`**:
  Opens the catalog filtered by a text query, or prompts for a search query in chat if omitted.
* **`/croppy items clear`**:
  Clears all active filters and search queries, returning to the first page of all items.

**Catalog Shortcut Controls (Administrative Spawner):**
* **Left-Click**: Spawn 1x item into inventory.
* **Right-Click**: Spawn a full stack (up to item maximum stack size, e.g. 64x).
* **Shift-Click**: Suggest `/croppy item give <category> <id> 1` pre-filled in chat.
* **Drop Key [Q]**: Drop 1x item on the ground at feet.
* **Control + Drop [Ctrl+Q]**: Drop full stack on the ground at feet.

Permission: `croppy.admin.items` (included in `croppy.admin`).

---

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
  Applies the specified fertilizer (e.g., `croppy:growth_fertilizer`, `croppy:quality_fertilizer`) directly to the targeted soil block, modifying its N-P-K nutrient reserves.
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

### Addon Lifecycle Management

```text
/croppy addon list
/croppy addon status <addonId>
/croppy addon load <addonId | all>
/croppy addon enable <addonId | all>
/croppy addon disable <addonId | all>
/croppy addon unload <addonId | all>
```

* **`/croppy addon list`**:
  Lists all discovered addons in `plugins/Croppy/addons/`, displaying their version, type (Java code addon or Declarative pack), and lifecycle state.
* **`/croppy addon status <addonId>`**:
  Inspects detailed metadata, loaded classes, registered species/items, active listeners, and task metrics for the specified addon.
* **`/croppy addon load <addonId | all>`**:
  Prepares an addon in memory and links its classloader hierarchy without activating gameplay mechanics (`LOADED` state).
* **`/croppy addon enable <addonId | all>`**:
  Enables the addon, resolves dependencies, registers its content in core registries, and invokes `onEnable` (`ENABLED` state).
* **`/croppy addon disable <addonId | all>`**:
  Safely executes the 5-phase decommissioning protocol, unregistering listeners and clearing domain registries while leaving classes in memory (`LOADED` state).
* **`/croppy addon unload <addonId | all>`**:
  Completely disables the addon, closes its `URLClassLoader`, and detaches graph references (`ON_DISK` state).

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
| `croppy.admin.items` | `op` | Access to the interactive item catalog GUI and administrative spawner (`/croppy items`). |
| `croppy.admin.addon` | `op` | Full access to `/croppy addon` lifecycle management subcommands. |
