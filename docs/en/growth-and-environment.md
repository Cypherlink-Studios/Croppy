---
title: Growth & Environment
description: Detailed breakdown of Croppy's deterministic growth simulation engine, environmental checks, hydration policies, and modifiers.
sidebar:
  order: 6
---

# Growth & Environment Simulation

Croppy replaces Minecraft's random block tick system with a deterministic, configurable **Growth Engine**. Rather than relying on sporadic game ticks, each crop evaluates its physical environment at regular intervals and accumulates progress points toward stage transitions.

---

## 1. The Growth Simulation Cycle

At each cycle (configured via `growth.tick-interval`, defaulting to 20 ticks / 1 second), the engine executes the following pipeline:

```text
+-----------------------------------------------------------------------------+
|                           GROWTH TICK PIPELINE                              |
+-----------------------------------------------------------------------------+
|                                                                             |
|  1. Chunk Validation   --> Checks if the crop's chunk is loaded in memory.  |
|                                                                             |
|  2. Maturity Check     --> If stage is already 'mature', skip processing.   |
|                                                                             |
|  3. Environment Check  --> Validates light level & water fluid levels.      |
|                            If not satisfied: growth stalls (no progress).   |
|                                                                             |
|  4. Hydration Policy   --> Checks daily watering record. If unwatered:      |
|                            applies UnwateredPolicy (stagnate or wither).    |
|                                                                             |
|  5. Modifier Pipeline  --> Calculates baseProgress * FertilizerModifiers.   |
|                                                                             |
|  6. Stage Progression  --> Adds progress. If points >= progress_required:   |
|                            advances stage & triggers DisplaySynchronizer.   |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Environmental Factors

### Light Level (`min_light`)
Crops require sufficient illumination for photosynthesis:
* Evaluates total block light (sunlight + artificial block light like torches and lanterns).
* If a species requires `min_light: 9` and the current block has light level `7`, the crop remains dormant until illuminated.
* Under-roof or greenhouse farming can achieve 24/7 growth by installing artificial lighting fixtures.

### Water Fluid & Moisture (`water_fluid_level`)
* **Surrounding Water Search**: Croppy scans horizontal blocks within a configurable radius (default: 4 blocks, covering a 9x9 farmland grid) for water sources, flowing water, or waterlogged blocks.
* Water source blocks yield a fluid level of `8`, while flowing water yields `1-7`.
* If a crop specifies `water_fluid_level: 1`, having at least one water channel or saturated farmland block within the radius satisfies this condition.

---

## 3. Daily Hydration & Unwatered Policies

When daily hydration tracking is enabled, crops monitor whether they have received water for the current world day. If a crop remains unwatered:

* **Stagnate (Default)**: The crop simply pauses growth for that cycle without taking damage.
* **Wither**: After consecutive unwatered days, crops can trigger a `WitherHandler`, degrading health or reverting stages until withered into dead bushes.

---

## 4. Growth Modifiers & Speed Boosts

The growth engine supports an extensible pipeline of `GrowthModifier` implementations:
* **Speed-Gro Fertilizers**: Apply multiplier bonuses (e.g., `1.25x` or `1.50x` growth speed) directly to the accumulated progress points per cycle.
* **Nutrient Richness**: Optimal soil N-P-K reserves provide additive bonuses, accelerating maturity.
