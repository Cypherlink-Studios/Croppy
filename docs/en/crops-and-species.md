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

### B. Virtual 3D Model Displays (`type: "model_3d"`)
Renders high-fidelity 3D item models with dynamic interaction hitboxes and yaw orientation using client-side **PacketEvents** virtual entities (`ITEM_DISPLAY` + `INTERACTION`).
* **Zero Ghost Entities**: Does not create persistent entities in Minecraft chunk NBT; entity packets are tracked and dispatched dynamically to viewers.
* **Placement Orientation**: Aligns the 3D model according to the player's viewing direction when planting.
* **Dynamic Hitbox Scaling**: Automatically resizes the interaction hitbox width and height as the crop progresses through its stages.

#### Rotation Modes (`rotation_mode`)
* `CARDINAL_4`: Snaps model yaw to the 4 cardinal directions (North, South, East, West).
* `CARDINAL_8`: Snaps model yaw to 8 directions (cardinal + diagonals).
* `FREE_YAW`: Preserves the exact viewing yaw of the player who planted the crop.
* `RANDOM_JITTER`: Applies a subtle randomized angle offset (±15°) to base placement yaw for organic crop field variation.

#### 3D Crop Example (`tomato.yml`)
```yaml
id: "croppy:tomato"
name: "<red>Tomato</red>"

display:
  type: "model_3d"
  base_block: "minecraft:air"
  rotation_mode: "RANDOM_JITTER"
  seed_item: "croppy:tomato_seeds"

stages:
  - id: "seed"
    progress_required: 50
    model:
      item_model: "croppy:tomato_stage_0"
      scale: [1.0, 1.0, 1.0]
      translation: [0.0, 0.0, 0.0]
      hitbox:
        width: 0.6
        height: 0.3

  - id: "sprout"
    progress_required: 100
    model:
      item_model: "croppy:tomato_stage_1"
      scale: [1.0, 1.0, 1.0]
      translation: [0.0, 0.0, 0.0]
      hitbox:
        width: 0.7
        height: 0.6

  - id: "mature"
    progress_required: 0
    model:
      item_model: "croppy:tomato_mature"
      scale: [1.0, 1.0, 1.0]
      translation: [0.0, 0.0, 0.0]
      hitbox:
        width: 0.8
        height: 0.9

requirements:
  min_light: 9
  water_fluid_level: 1

harvest:
  yield:
    - item: "croppy:tomato"
      min: 1
      max: 3
    - item: "croppy:tomato_seeds"
      min: 1
      max: 2
```

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
