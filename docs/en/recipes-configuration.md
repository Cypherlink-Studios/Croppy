---
title: Culinary Recipes Configuration
description: Technical specification and YAML configuration guide for culinary recipes, ingredient matrices, processes, and discovery conditions.
sidebar:
  order: 13
---

# Culinary Recipes Configuration

Culinary recipes in Croppy define how agricultural crops, spices, and base items are combined within physical cooking stations to produce food delicacies. Recipes are stored as modular YAML files in the `plugins/Croppy/recipes/` directory.

The engine parses these configurations via `YamlRecipeLoader`, registering them into the thread-safe `RecipeRegistry` for station processing, anti-shadowing matching, and discovery tracking.

---

## 1. Complete YAML Schema Breakdown

Here is a fully documented example representing `plugins/Croppy/recipes/tomato_soup.yml`:

```yaml
# Unique namespaced semantic key (<namespace>:<value>)
id: "croppy:tomato_soup"

# Display name formatted with MiniMessage tags
name: "<gradient:#ff5555:#ffaa00>Tomato Soup</gradient>"

# Category key used for filtering in the Culinary Compendium GUI
# Default: "croppy:all" if omitted
category: "croppy:soups"

# Physical station definition required to prepare this recipe
station: "croppy:cooking_pot"

# Sequential process ID tracking preparation states
# Default: falls back to the recipe 'id' if omitted
process: "croppy:tomato_soup_cook"

# List of required input ingredients
inputs:
  - item: "croppy:tomato"
    amount: 2
  - item: "minecraft:bowl"
    amount: 1

# Output dish dispatched when cooking succeeds
result:
  item: "croppy:tomato_soup"
  amount: 1

# Recipe discovery engine configuration
discovery:
  # Whether the station accepts surplus ingredients beyond required inputs
  allow_excess_inputs: false
  
  # Discovery triggers enabled for unlocking this recipe
  methods:
    - "croppy:experimental"  # Unlocks when successfully cooked blindly
    - "croppy:automatic"     # Unlocks automatically upon holding main ingredients

  # Culinary hint presented when the recipe is in HINTED state
  hint:
    description: "<gray>A warm simmered red soup cooked from ripe garden tomatoes and served in a bowl.</gray>"
    revealed_ingredients:
      - "croppy:tomato"
    hide_result: true

# Custom extensible key-value properties
properties:
  difficulty: "easy"
  simmer_temperature: "95.0"
```

---

## 2. Field Reference Table

| Field Path | Type | Required | Default | Description |
|---|---|---|---|---|
| `id` | String (`Key`) | **Yes** | — | Unique identifier in `<namespace>:<value>` format (e.g. `croppy:tomato_soup`). |
| `name` | String | **Yes** | — | Player-facing display name supporting MiniMessage tags. |
| `category` | String (`Key`) | No | `croppy:all` | Compendium category key for navigation tab filtering. |
| `station` | String (`Key`) | **Yes** | — | Station key where this recipe can be prepared (`croppy:cooking_pot`, `croppy:skillet`). |
| `process` | String (`Key`) | No | Same as `id` | Identifier of the [Sequential Cooking Process](cooking-processes.md) governing durations, phases, particles, and holograms. |
| `inputs` | List of Maps | **Yes** | `[]` | List of required ingredients. Each entry requires `item` (Key) and optional `amount` (integer, default `1`). |
| `result.item` | String (`Key`) | No | Same as `id` | The produced item identifier (e.g. `croppy:tomato_soup` or `minecraft:mushroom_stew`). |
| `result.amount` | Integer | No | `1` | Number of items produced upon successful completion. |
| `discovery.allow_excess_inputs` | Boolean | No | `false` | When `false`, cooking fails if extra unrelated items are inside the station. |
| `discovery.methods` | List of Strings | No | `[]` | Enabled discovery SPI methods (e.g., `croppy:experimental`, `croppy:automatic`). |
| `discovery.hint.description` | String | No | Default hint | MiniMessage lore hint displayed in the Culinary Compendium when `HINTED`. |
| `discovery.hint.revealed_ingredients`| List of Strings | No | `[]` | List of ingredient keys visible in the compendium while in `HINTED` state. |
| `discovery.hint.hide_result` | Boolean | No | `true` | If `true`, the output icon and title remain masked while `HINTED`. |
| `properties` | Map<String, String> | No | `{}` | Extensible metadata key-value pairs (e.g., cooking guidelines, difficulty grades). |

---

## 3. Discovery & Hint Mechanics

Croppy uses an anti-spoiler progression system configured through the `discovery` block:

```text
+-----------------------------------------------------------------------------+
|                          RECIPE PROGRESSION PIPELINE                        |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [UNKNOWN]     --> Compendium displays a locked mystery icon ("???").       |
|       |                                                                     |
|       v                                                                     |
|  [HINTED]      --> Shows 'hint.description' and 'revealed_ingredients'.     |
|       |            The final dish remains hidden if 'hide_result: true'.    |
|       v                                                                     |
|  [DISCOVERED]  --> Full recipe viewable: all ingredients, ratios & station. |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### Discovery Methods
* `croppy:experimental`: Enables kitchen trial-and-error. When a chef places the exact ingredients into a heated station and finishes cooking, the recipe automatically unlocks permanently.
* `croppy:automatic`: Unlocks the recipe as soon as the player acquires the prerequisite harvest crops or tools in their inventory.

---

## 4. Step-by-Step Tutorial: Creating a Custom Recipe

Let's author a new skillet recipe: **Carrot Omelette** (`croppy:carrot_omelette`).

### Step 1: Create the Recipe YAML
Create `plugins/Croppy/recipes/carrot_omelette.yml`:

```yaml
id: "croppy:carrot_omelette"
name: "<gradient:#ffaa00:#ffff55>Carrot Omelette</gradient>"
category: "croppy:fried"
station: "croppy:skillet"

inputs:
  - item: "minecraft:egg"
    amount: 1
  - item: "minecraft:carrot"
    amount: 2

result:
  item: "croppy:carrot_omelette"
  amount: 1

discovery:
  allow_excess_inputs: false
  methods:
    - "croppy:experimental"
  hint:
    description: "<gray>Fold farm-fresh eggs with sweet diced carrots over high heat.</gray>"
    revealed_ingredients:
      - "minecraft:carrot"
    hide_result: false

properties:
  difficulty: "medium"
```

### Step 2: Ensure Items Exist
Verify that `croppy:carrot_omelette` is defined under `plugins/Croppy/items/ingredients/carrot_omelette.yml` (or references a vanilla item like `minecraft:cooked_chicken`).

### Step 3: Reload the Server
Run the administrative command in game or from the console:
```text
/croppy reload
```
The server will log:
```text
[Croppy] Registered 4 recipe definitions into RecipeRegistry.
```

Your new recipe is now active, ready for experimental discovery or inspection via `/croppy recipes all`!
