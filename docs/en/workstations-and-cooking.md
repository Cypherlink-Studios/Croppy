---
title: Workstations & Cooking
description: Guide to physical cooking stations (Cooking Pot, Skillet), 3D item placement, thermal simmering, and burn mechanics.
sidebar:
  order: 9
---

# Workstations & Cooking Mechanics

Croppy elevates cooking from instant crafting grid conversions into an interactive, multi-sensory culinary process. Chefs interact with physical cooking stations in the world, observe ingredients simmering via PacketEvents virtual displays, and manage heat to avoid burning their dishes.

---

## 1. Supported Cooking Workstations

Croppy includes two dedicated cooking stations with unique thermal capacities and interaction styles:

```text
+-----------------------------------------------------------------------------+
|                          COOKING WORKSTATIONS                               |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [COOKING POT] (croppy:cooking_pot)                                         |
|  - Block: Player Head (Custom Cauldrons/Pots)                               |
|  - Interaction: HYBRID (Physical click or 9xN GUI menu)                     |
|  - Capacity: 6 Input Ingredients, 2 Output Dispatched Items                 |
|  - Ideal For: Soups, stews, sauces, jams, and simmering broths             |
|                                                                             |
|  [SKILLET] (croppy:skillet)                                                 |
|  - Block: Heavy Weighted Pressure Plate                                     |
|  - Interaction: PHYSICAL (Direct right-click item insertion & extraction)   |
|  - Capacity: 4 Input Ingredients, 2 Output Dispatched Items                 |
|  - Ideal For: Searing meats, stir-fries, pancakes, and fried delicacies     |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Virtual Item & Text Displays (PacketEvents)

Unlike traditional plugins that spawn live Armor Stands or Minecraft entities (which cause server lag, chunk corruption, and ghost entities upon crashes), Croppy uses **PacketEvents**:

* **Ingredient Previews**: When items are placed into a skillet or pot, a client-side virtual Item Display renders the ingredients floating or sizzling inside the pan with custom scale, translation, and pitch.
* **Floating Status Holograms**: A real-time billboard text display floats above the station, dynamically updating:
  * Dish Name (`{recipe_name}`)
  * Animated Progress Bar (`{progress_bar}`) & Percentage (`{progress_percent}%`)
  * Current Temperature (`{temperature_c}`) and Active Thermal Zone (`{thermal_zone}`)
* **Distance Culling**: Virtual displays automatically cull outside their configured view distance (`8-24 blocks`), keeping client FPS silky smooth.

---

## 3. Interactive Cooking Flow & Process Execution

Croppy v0.5.0 features dynamic cooking workflows tailored to station interaction models:

### Skillet Workflow (Physical Interaction)
1. **Heat the Skillet**: Place the skillet atop an active heat source (e.g., lit Campfire or Stove). Allow temperature to reach the recipe's minimum threshold (e.g., 120°C for fried eggs).
2. **Place Ingredients**: Right-click the skillet holding raw ingredients (e.g., an egg). The item snaps onto the sizzling cooking surface via client-side PacketEvents virtual item displays.
3. **Automatic Process Trigger**: Once recipe input requirements and thermal start conditions are satisfied, the sequential cooking process kicks off automatically.
4. **Retrieve Dish**: When progress hits 100%, right-click with an empty hand to collect your freshly cooked delicacy!

### Cooking Pot Workflow (Hybrid / GUI Menu)
1. **Open the Pot Menu**: Right-click the cooking pot over a heated campfire or stove to open its interactive 9xN matrix menu.
2. **Insert Ingredients**: Place your vegetables, meats, broth, or bowls into the input slots.
3. **Ignite Cookware (Button 'B')**: Click the **Ignite Cookware (`START_PROCESS`)** action button:
   * The engine validates that ingredients match a valid recipe and thermal start conditions are satisfied.
   * Input ingredients are immediately consumed from the input matrix.
   * Input slots lock and update, and the process initiates with live audio and splash particles.
4. **Collect Output Dishes (`populateOutputContents`)**: When the timer finishes, the final dishes automatically populate the station's output slots. If the output slots are already full, dishes safely drop onto the ground beside the pot to prevent item loss.

### Thermal Hold & Mid-Cook Safety
If the heat source beneath a station goes out mid-cooking, the active process automatically **pauses** (`pause_on_condition_failure`). Cooking progress and elapsed seconds remain safely preserved until the heat source is reignited.

---

## 4. Cooking Rules & Burn Mechanics

Cooking in Croppy requires attentiveness. Station rules monitor temperature and process duration:

* **Simmering & Heat Requirement**: Recipes require specific thermal zones (e.g., Warm, Simmering, Boiling). A cold cooking pot will not advance recipe progress.
* **Overcooking Burn (`rules/overcooking_burn.yml`)**: Leaving a finished dish on high heat after it reaches 100% completion will trigger overcooking. If not removed in time, the dish ruins into burnt residue or charcoal.
* **Thermal Burn (`rules/thermal_burn.yml`)**: Exposing delicate ingredients to extreme heat levels beyond their tolerance causes thermal scorch, instantly spoiling the recipe.

---

## 5. Directional Placement & Item Stacking

* **Automatic Facing**: Workstations placed in the world dynamically face towards the player using cardinal snapping (`CARDINAL_4`, `CARDINAL_8`) or continuous angle (`FREE_YAW`). Virtual 3D models and child process item displays rotate seamlessly to match this perspective.
* **Native Item Stacking**: Ingredients retrieved or extracted from workstations preserve native vanilla item metadata. Vanilla crops, meats, and seasonings extracted from pots or skillets stack cleanly with identical items already in player inventories.
