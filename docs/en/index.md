---
title: Introduction & Overview
description: Complete overview, core simulation features, architecture, and gameplay highlights for Croppy.
sidebar:
  order: 1
---

# Croppy Documentation Hub

Welcome to the official documentation for **Croppy**, an extensible, modular agriculture, ingredients, processing stations, and culinary simulation engine designed for modern Minecraft Paper servers (1.21.8+).

Croppy bridges the gap between traditional vanilla farming and immersive, deep culinary gameplay. It turns farming into a living ecosystem: seeds demand specific sunlight and moisture, soil nutrients deplete and require fertilization, physical workstations cook ingredients over dynamic heat sources, and chefs discover secret recipes in an interactive culinary compendium.

---

## Core Gameplay Highlights

```text
+-----------------------------------------------------------------------------+
|                               CROPPY HIGHLIGHTS                             |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [1. LIVING AGRICULTURAL SIMULATION]                                        |
|  Crops evaluate light levels, soil moisture, ambient temperature, and       |
|  greenhouse enclosures rather than relying on vanilla random block ticks.   |
|                                                                             |
|  [2. SOIL NUTRIENT DYNAMICS (N-P-K)]                                        |
|  Farmland holds Nitrogen, Phosphorus, and Potassium reserves that deplete   |
|  during growth cycles and require replenishment via organic fertilizers.    |
|                                                                             |
|  [3. MANUAL & PROGRESSIVE WATERING]                                         |
|  Tiered watering cans hydrate multi-block grids with custom capacities,     |
|  moisture retention properties, and natural evaporation rates.              |
|                                                                             |
|  [4. PHYSICAL COOKING WORKSTATIONS]                                         |
|  Place skillets and cooking pots directly on heat sources with 3D item       |
|  displays, timed simmering, and realistic thermal burn mechanics.           |
|                                                                             |
|  [5. VARIABLE HEAT SOURCES & FUELS]                                         |
|  Stoves and fire pits consume solid fuels to generate temperature curves    |
|  that transfer heat to overlying cooking stations.                          |
|                                                                             |
|  [6. RECIPE DISCOVERY & CULINARY COMPENDIUM]                                |
|  Master a cookbook containing categorized dishes, unlock hidden recipes     |
|  through culinary experimentation, and inspect required ingredients.        |
|                                                                             |
|  [7. NATIVE MULTI-LANGUAGE SUPPORT (i18n)]                                  |
|  All messages, action bars, tooltips, and item names render in each         |
|  player's preferred Minecraft client language with Crowdin synchronization. |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## Architectural Principles

Croppy is engineered from the ground up following **Hexagonal Architecture (Ports and Adapters)**:

- **Zero Bukkit in Core**: The domain engine (`croppy-core`) and public contracts (`croppy-api`) are completely decoupled from platform APIs. They execute pure Java 21 logic, enabling fast unit testing and long-term maintainability.
- **Paper Adapter**: `croppy-paper` translates platform events, manages packet-level virtual displays, schedules asynchronous database tasks, and handles user interactions.
- **Fail-Fast Configuration**: All YAML configurations, species definitions, recipes, and menus are validated strictly at startup to catch mistakes before players join.

---

## Core Terminology

- **Species**: The logical definition of a crop type (`croppy:wheat`, `croppy:carrot`), defining its lifecycle stages, environmental tolerances, nutrient appetite, and harvest loot.
- **Crop Instance**: An active crop growing in the world, bound to a specific block coordinate with tracked progress, health, and age.
- **Farmland Moisture**: A discrete hydration level (0 to 8) maintained on tilled farmland, affected by watering cans, rain, and evaporation.
- **Soil Nutrients**: The tri-nutrient reserve of Nitrogen (N), Phosphorus (P), and Potassium (K) present in farmland soil blocks.
- **Workstation**: A physical station block (e.g., cooking pot, skillet) capable of holding ingredients and executing transformation recipes.
- **Heat Source**: A station component (such as a wood-fired stove) that consumes fuels and supplies ambient thermal energy to adjacent or overlying workstations.
- **Culinary Compendium**: The in-game recipe encyclopedia (`croppy:recipe_book`) allowing players to browse discovered culinary preparations, categorized by meal types.
