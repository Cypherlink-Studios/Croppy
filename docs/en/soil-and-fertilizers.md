---
title: Soil & Fertilizers
description: Guide to soil nutrient chemistry (N-P-K), fertilizer items, application methods, and moisture retention hydrogels.
sidebar:
  order: 7
---

# Soil Chemistry & Fertilizers

Soil in Croppy is more than static dirt—it acts as an active biochemical reservoir. Tilled farmland stores nutrient reserves and can be treated with specialized organic and chemical fertilizers to boost growth rates, elevate harvest quality, or lock in daily moisture.

---

## 1. Soil Nutrients (N-P-K Model)

Farmland blocks track three fundamental macronutrients essential for plant development:

* **Nitrogen (N)**: Governs vegetative growth, leaf formation, and early sprout speed.
* **Phosphorus (P)**: Stimulates root establishment, flower blooming, and transition speed between middle stages.
* **Potassium (K)**: Increases disease resistance, overall crop health, and the final quality grade of harvested produce.

When crops progress through growth stages, they draw nutrients from the underlying farmland. Farmers can inspect soil nutrient levels using the **Crop & Soil Inspector** tool or apply targeted fertilizers to replenish depleted minerals.

---

## 2. Fertilizer Types & Effects

Croppy bundles three distinct classes of specialized fertilizers:

| Fertilizer Key | Base Material | Classification | Primary Effect |
|---|---|---|---|
| `croppy:speed_gro` | `BONE_MEAL` | **`SPEED`** | Accelerates crop growth rate by **+25%** (`speed_multiplier: 1.25`). |
| `croppy:quality_fertilizer` | `GLOWSTONE_DUST` | **`QUALITY`** | Grants a **+20% bonus** (`quality_boost: 0.20`) to higher-tier harvest yields. |
| `croppy:retaining_soil` | `CLAY_BALL` | **`RETAINING`** | Provides a **33% chance** (`retention_chance: 0.33`) for soil to remain hydrated overnight without watering. |

---

## 3. How to Apply Fertilizers

1. **Prepare the Farmland**: Till a dirt block using any hoe.
2. **Apply Before or After Planting**:
   Hold the fertilizer item in your main hand and **Right-Click** the farmland block.
3. **Visual Confirmation**: Green and gold particle effects erupt from the soil, confirming that the fertilizer attributes and nutrient adjustments have bound to the block's Persistent Data Container (PDC).
4. **Limits**: Each soil block can support one active primary fertilizer enhancement at a time to prevent exploit stacking.
