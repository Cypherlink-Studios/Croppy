---
title: Crops & Species
description: Complete guide to configuring crop species in YAML, lifecycle stages, environmental requirements, and harvest loot.
sidebar:
  order: 5
---

# Crops & Species Configuration

In Croppy, agricultural varieties are declared as logical **Species** within `plugins/Croppy/crops/<name>.yml`. Croppy separates the logical simulation state from Minecraft's physical block representation, enabling custom growth curves, multi-stage lifecycles, and tailored harvest drop tables.

---

## 1. Species YAML Schema Breakdown

Here is a full breakdown using `wheat.yml` as a baseline:

```yaml
# Unique semantic identifier in <namespace>:<value> format
id: "croppy:wheat"

# Default display name (can be localized via i18n catalogs)
name: "Wheat"

display:
  # Display backing model: "vanilla" (block state age) or "custom"
  type: "vanilla"
  # Vanilla block type placed in the world
  block: "minecraft:wheat"
  # Custom item used by players to plant this species
  seed_item: "croppy:wheat_seeds"

stages:
  - id: "seed"
    progress_required: 100
    display:
      block_age: 0

  - id: "sprout"
    progress_required: 100
    display:
      block_age: 2

  - id: "growing"
    progress_required: 150
    display:
      block_age: 5

  - id: "mature"
    progress_required: 0
    display:
      block_age: 7

requirements:
  # Minimum block light level (0-15) required for photosynthesis
  min_light: 9
  # Minimum moisture/fluid level required in surrounding soil
  water_fluid_level: 1

harvest:
  # Items dropped when the mature crop is harvested or broken
  yield:
    - item: "minecraft:wheat"
      min: 1
      max: 1
    - item: "croppy:wheat_seeds"
      min: 1
      max: 3
```

---

## 2. Display Backing Modes

Croppy supports two primary display strategies for crops:

### A. Vanilla-Backed (`type: "vanilla"`)
Maps crop stages directly to vanilla Minecraft block state ages (`block_age: 0..7`).
* Works natively without client resource packs.
* Supported blocks include `minecraft:wheat`, `minecraft:carrots`, `minecraft:potatoes`, `minecraft:beetroots`, and `minecraft:pitcher_crop`.
* Block updates occur seamlessly when a crop crosses stage thresholds.

### B. Custom Item Displays
Allows modeling non-vanilla agricultural crops using invisible base blocks combined with Item Displays or custom model data. This mode is used when expanding beyond standard vanilla flora.

---

## 3. Growth Stages & Progress Points

Each stage defines an `id` and `progress_required`:
* When a crop accumulates points equal to `progress_required`, it transitions to the subsequent stage.
* The final stage (`mature`) sets `progress_required: 0`, signaling that the crop is fully grown and ready for harvest.
* During stages, crops consume environmental nutrients and moisture, making multi-stage growth a dynamic journey rather than random ticks.

---

## 4. Harvest Yield Tables

The `harvest.yield` block accepts multiple drop definitions:
* **`item`**: A vanilla Minecraft item ID (`minecraft:wheat`) or a custom Croppy item key (`croppy:wheat_seeds`).
* **`min` & `max`**: Integer drop bounds calculated via random uniform distribution upon harvest.
