---
title: Crafting Table Recipes
description: Declarative Bukkit crafting recipe system for custom stations, tools, watering cans, and items at standard Minecraft crafting tables.
sidebar:
  order: 22
---

# Crafting Table Recipes

Croppy provides a fully declarative Bukkit crafting recipe system allowing custom workstations, agricultural tools, tiered watering cans, and fertilizers to be crafted natively at standard Minecraft crafting tables in Survival mode.

Crafting table recipes are stored as modular YAML files in `plugins/Croppy/crafting/` (separate from physical culinary recipes located in `plugins/Croppy/recipes/`).

---

## 1. Crafting Recipes vs Culinary Recipes

It is important to distinguish between the two recipe subsystems in Croppy:

| Feature | Crafting Table Recipes (`crafting/`) | Culinary Recipes (`recipes/`) |
|---|---|---|
| **Location** | `plugins/Croppy/crafting/**/*.yml` | `plugins/Croppy/recipes/**/*.yml` |
| **Workstation** | Vanilla Minecraft Crafting Table (3x3 grid) | Physical Croppy stations (Cooking Pot, Skillet, Stove) |
| **System** | Native Bukkit `ShapedRecipe` / `ShapelessRecipe` | Custom multi-phase culinary simulation engine |
| **Recipe Book** | Integrated into vanilla Minecraft Recipe Book | Tracked in the Culinary Compendium GUI (`/croppy recipes`) |
| **Use Case** | Assembling cookware, forging watering cans, crafting tools | Simmering soups, searing meat, baking dishes |

---

## 2. Recipe Types & YAML Schema

Crafting recipe files support two primary types: **`shaped`** and **`shapeless`**.

### 2.1 Shaped Recipe Example

A shaped recipe enforces an exact grid arrangement (from 1x1 up to 3x3).

```yaml
# Unique namespaced recipe identifier
id: "croppy:cooking_pot"

# Recipe type: shaped
type: "shaped"

# Optional recipe book grouping identifier
group: "croppy_stations"

# Optional recipe book category: misc, equipment, building, redstone (default: misc)
category: "misc"

# Resulting item specification (defaults to id with amount 1 if omitted)
result:
  item: "croppy:cooking_pot"
  amount: 1

# Grid pattern matrix (1 to 3 rows, 1 to 3 characters per row)
shape:
  - "I I"
  - "ICI"
  - " I "

# Character mapping to ingredient choices
ingredients:
  I: "minecraft:iron_ingot"
  C: "minecraft:cauldron"
```

### 2.2 Shapeless Recipe Example

A shapeless recipe matches any arrangement of ingredients on the grid (up to 9 items).

```yaml
id: "croppy:growth_fertilizer"
type: "shapeless"
group: "croppy_fertilizers"
category: "misc"

result:
  item: "croppy:growth_fertilizer"
  amount: 2

ingredients:
  - "minecraft:bone_meal"
  - "minecraft:rotten_flesh"
```

---

## 3. Ingredient Choices & Resolution

Croppy automatically resolves ingredient declarations into platform-native `RecipeChoice` instances:

### 1. Vanilla Materials
* Format: `"minecraft:material_name"` or `"material_name"` (e.g. `"minecraft:iron_ingot"`, `"campfire"`).
* Mapped to: Bukkit `RecipeChoice.MaterialChoice`.

### 2. Vanilla Tags
* Format: `"#minecraft:tag_name"` or `"#tag_name"` (e.g. `"#minecraft:planks"`, `"#wool"`).
* Mapped to: Bukkit `RecipeChoice.MaterialChoice` accepting any block or item material matching the tag.

### 3. Custom Croppy Items (ExactChoice with PDC Preservation)
* Format: `"croppy:item_id"` (e.g. `"croppy:wooden_can"`, `"croppy:cooking_pot"`).
* Mapped to: Bukkit `RecipeChoice.ExactChoice` built via `ItemFactory`.
* **Security & Fidelity**: Exact matching validates PersistentDataContainer tags, custom model data, and display attributes, ensuring that only authentic Croppy items can be consumed as upgrade ingredients.

---

## 4. Progressive Tier Upgrades

Croppy's crafting system enables tiered item progression. For example, higher-tier watering cans require the preceding tier:

```
[Wooden Can] + Copper Ingots ──> [Copper Can]
[Copper Can] + Iron Ingots   ──> [Iron Can]
[Iron Can]   + Gold Ingots   ──> [Golden Can]
[Golden Can] + Diamonds      ──> [Diamond Can]
[Diamond Can] + Netherite     ──> [Netherite Can]
```

Example recipe for upgrading to `croppy:copper_can`:

```yaml
id: "croppy:copper_can"
type: "shaped"
group: "croppy_watering_cans"
category: "equipment"
result: "croppy:copper_can"
shape:
  - "C C"
  - "CWC"
  - " C "
ingredients:
  C: "minecraft:copper_ingot"
  W: "croppy:wooden_can"
```

---

## 5. Recipe Book Auto-Discovery

By default, Croppy automatically teaches all registered custom crafting recipes to players when they join the server:

```yaml
# plugins/Croppy/config.yml
crafting:
  # Automatically discover all registered custom recipes in the player's recipe book upon joining
  auto-discover-recipes: true
```

When set to `false`, players unlock recipes only when they obtain the requisite ingredients according to vanilla criteria or through server commands (`/recipe give <player> <recipe>`).

---

## 6. Addon Declarative Recipe Discovery

Both compiled JAR addons and declarative content packs can declare custom crafting recipes:

* Place YAML recipe files in `<addon-root>/crafting/**/*.yml`.
* Croppy parses and registers them into `CraftingRecipeRegistry` upon addon enable.
* All addon recipe keys are tracked in `AddonContext` and cleanly unregistered when the addon is disabled or reloaded.

---

## 7. Bundled Starter Recipes

Croppy bundles default starter crafting recipes extracted into `plugins/Croppy/crafting/`:

* **Workstations**:
  * `crafting/stations/cooking_pot.yml` (Cauldron + 4 Iron Ingots)
  * `crafting/stations/stove.yml` (Smooth Stone Slabs + Bricks + Campfire)
  * `crafting/stations/skillet.yml` (2 Iron Ingots + Stick)
* **Agricultural Tools**:
  * `crafting/tools/inspector.yml` (Glass Pane + Compass + Stick)
  * `crafting/tools/thermometer.yml` (Glass Pane + Redstone + Stick)
* **Tiered Watering Cans**:
  * `crafting/watering_cans/wooden_can.yml` (Planks tag + Bowl)
  * `crafting/watering_cans/copper_can.yml` (Copper Ingots + Wooden Can)
  * `crafting/watering_cans/iron_can.yml` (Iron Ingots + Copper Can)
  * `crafting/watering_cans/golden_can.yml` (Gold Ingots + Iron Can)
  * `crafting/watering_cans/diamond_can.yml` (Diamonds + Golden Can)
  * `crafting/watering_cans/netherite_can.yml` (Netherite Ingot + Diamond Can)
* **Fertilizers**:
  * `crafting/fertilizers/growth_fertilizer.yml` (Bone Meal + Rotten Flesh)
  * `crafting/fertilizers/hydrating_fertilizer.yml` (Bone Meal + Clay Ball)
  * `crafting/fertilizers/quality_fertilizer.yml` (Bone Meal + Glowstone Dust)
