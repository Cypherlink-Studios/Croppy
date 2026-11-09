---
title: Watering Mechanics
description: Comprehensive guide to tiered watering cans, area-of-effect hydration, water refill sources, and moisture retention.
sidebar:
  order: 8
---

# Watering Mechanics & Hydration

Water is the lifeblood of agricultural cultivation in Croppy. Farmland moisture directly determines whether crops accumulate growth points each day. To support farms of all scales, Croppy introduces a tiered progression of **Watering Cans** featuring custom capacities and expanding Area of Effect (AoE) patterns.

---

## 1. Watering Can Tiers & Specifications

Watering cans store discrete water charges within their Persistent Data Container (PDC). As farmers upgrade their tools, both water capacity and the hydration footprint expand:

| Item Key | Display Name | Base Material | Water Capacity | Hydration Area (AoE) |
|---|---|---|---|---|
| `croppy:wooden_can` | **Wooden Watering Can** | `WOODEN_HOE` | **20 uses** | **1x1** (Single block) |
| `croppy:copper_can` | **Copper Watering Can** | `COPPER_INGOT` | **50 uses** | **1x3** (Linear 3 blocks) |
| `croppy:iron_can` | **Iron Watering Can** | `IRON_INGOT` | **100 uses** | **1x3** (Linear 3 blocks) |
| `croppy:golden_can` | **Golden Watering Can** | `GOLD_INGOT` | **250 uses** | **3x3** (Square 9 blocks) |
| `croppy:diamond_can` | **Diamond Watering Can** | `DIAMOND` | **500 uses** | **3x3** (Square 9 blocks) |
| `croppy:netherite_can` | **Netherite Watering Can** | `NETHERITE_INGOT` | **1000 uses** | **5x5** (Square 25 blocks) |

---

## 2. Refilling Watering Cans

Watering cans consume one charge per pour action. When empty, a warning appears in the action bar (`watering.empty`).

To refill a watering can to maximum capacity:
1. Locate any valid water source:
   * Natural water source block (`minecraft:water`), ocean, river, or pond.
   * **Waterlogged blocks** (slabs, stairs, fences, leaves, chest waterlogging).
   * **Water-filled Cauldrons** (`WATER_CAULDRON` with water level > 0).
2. Hold the watering can in your main hand and **Right-Click** the water target.
3. **Fluid Raytracing**: You can also right-click in the air facing a water surface within a 5-block reach distance. Server-side fluid raytracing will accurately detect the liquid and replenish the can.
4. A splash sound and bubble particles confirm the refill, restoring the can to its maximum capacity.

---

## 3. Farmland Hydration Dynamics

* **Hydration Levels (0 to 8)**: When poured on farmland, Croppy elevates the block's moisture level to maximum (`8/8`).
* **Moisture Evaporation**: Each morning or during dry weather cycles, farmland loses moisture unless sustained by adjacent water channels or protected with `croppy:hydrating_fertilizer`.
* **Daily Watered Check**: Crops inspect whether their block was hydrated during the current world day. Keeping soil consistently moist guarantees uninterrupted growth cycles.
