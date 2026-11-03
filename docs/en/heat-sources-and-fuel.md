---
title: Heat Sources & Fuel
description: In-depth guide to thermal heat sources, stoves, fuel combustion, heating/cooling curves, and temperature management.
sidebar:
  order: 11
---

# Heat Sources & Fuel Combustion

Culinary stations such as cooking pots and skillets require active thermal energy to prepare dishes. Rather than static binary states (on/off), Croppy implements continuous thermodynamic curves modeled through **Heat Source Stations** like the Rustic Stove.

---

## 1. The Rustic Stove (`croppy:stove`)

The stove serves as the primary heat generator in culinary setups:

```text
+-----------------------------------------------------------+
|                    CULINARY STACK SETUP                   |
+-----------------------------------------------------------+
|                                                           |
|       [COOKING POT / SKILLET]  <-- Receives transferred   |
|                 |                   thermal energy        |
|                 v                                         |
|          [RUSTIC STOVE]        <-- Burns solid fuels,     |
|                                     generates heat        |
+-----------------------------------------------------------+
```

### Thermal Specification
* **Minimum / Ambient Temperature**: `20.0°C` (Room temperature)
* **Maximum Safe Peak**: `350.0°C`
* **Heating Rate**: `+5.0°C` per tick cycle while fuel is actively burning
* **Cooling Rate**: `-2.0°C` per tick cycle when unlit, dissipating heat naturally back to ambient
* **Combustion Multiplier**: `1.0x` baseline

---

## 2. Fuel Types & Calorific Values

Stoves can burn standard vanilla fuels or custom Croppy fuel items inserted via physical interaction or the stove's internal GUI (`croppy:stove_menu`):

| Fuel Type | Burn Duration (Ticks) | Thermal Zone Produced |
|---|---|---|
| **Wood Planks / Sticks** | 300 - 600 ticks | **Warm** (60°C - 100°C) |
| **Coal / Charcoal** | 1,600 ticks | **Simmering / Medium** (100°C - 180°C) |
| **Dried Kelp Block** | 4,000 ticks | **Boiling / High** (180°C - 240°C) |
| **Blaze Rod** | 2,400 ticks | **Extreme Searing** (240°C - 320°C) |

---

## 3. Operating the Stove

1. **Place the Stove**: Set down the Rustic Stove block.
2. **Mount the Cooking Station**: Place your Cooking Pot or Skillet directly on top of the stove block.
3. **Insert Fuel**:
   * **Right-Click** the stove to open its 9xN fuel interface, or hold fuel in hand and sneak-right-click to load directly into the firebox.
4. **Ignite**: Light the fuel using Flint and Steel or a fire-starting prompt.
5. **Monitor Heat**: Watch the floating hologram above the attached cooking pot or use the **Culinary Thermometer** tool (`/croppy item give tools thermometer 1`) to check the exact Celsius reading.
