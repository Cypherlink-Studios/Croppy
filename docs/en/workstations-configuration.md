---
title: Workstations Configuration
description: Technical specification and YAML configuration guide for physical cooking stations, PacketEvents 3D virtual displays, and thermal thermodynamic curves.
sidebar:
  order: 10
---

# Workstations Configuration

Cooking and processing workstations in Croppy are physical blocks placed in the Minecraft world that interact with recipes, heat sources, and players. Workstations are defined as modular YAML files in the `plugins/Croppy/stations/` directory.

The engine parses these configurations via `YamlStationLoader`, creating immutable `StationDefinition` instances that govern interaction models, item capacities, client-side PacketEvents virtual entities, and thermodynamic properties.

---

## 1. Complete YAML Schema Breakdown

Here is a fully documented example representing `plugins/Croppy/stations/cooking_pot.yml`:

```yaml
# Unique namespaced station key
id: "croppy:cooking_pot"

# Display name with MiniMessage formatting
name: "<gradient:#ffaa00:#ff5555>Cooking Pot</gradient>"

# Physical Minecraft block backing this workstation in the world
block: "minecraft:player_head"

# Directional facing mode when placed by players: CARDINAL_4, CARDINAL_8, or FREE_YAW
rotation_mode: "CARDINAL_4"

# Interaction paradigm: PHYSICAL, GUI, or HYBRID
interaction: "HYBRID"

# Container capacities
input_capacity: 6
output_capacity: 2

# Associated inventory menu definition from plugins/Croppy/menus/
menu: "croppy:cooking_pot"

# Master toggle for PacketEvents client-side virtual displays
has_display: true

# Client-side PacketEvents virtual display layers
visual:
  # 3D Item Display: Renders simmering ingredients inside the station
  process_display:
    enabled: true
    type: "ITEM"
    translation: [0.0, 0.35, 0.0]
    scale: [0.55, 0.15, 0.55]
    view_distance: 24
    interpolation_duration: 3

  # Text Display Hologram: Real-time status floating above the station
  text_display:
    enabled: true
    translation: [0.0, 1.35, 0.0]
    billboard: "CENTER"
    view_distance: 8
    line_width: 220
    format: |
      <gradient:#ffaa00:#ff5555><bold>{recipe_name}</bold></gradient>
      {progress_bar} <white>{progress_percent}%</white>
      <gray>Heat: <aqua>{temperature_c}°C</aqua> (<gold>{thermal_zone}</gold>)</gray>

# Thermal and operational properties
properties:
  thermal: "true"
  has_thermal: "true"
```

---

## 2. Dynamic Placement & Rotation Modes

When a player places a workstation block in the world, the engine dynamically calculates the placement yaw so the front of the station faces towards the player (`playerYaw + 180°`).

The snapping behavior is governed by the `rotation_mode` attribute (alias: `rotation`):

| Rotation Mode | Snapping Angle | Description |
|---|---|---|
| `CARDINAL_4` | 90° intervals | Snaps to North, South, East, or West. Best for standard square workstation blocks. *(Default)* |
| `CARDINAL_8` | 45° intervals | Snaps to 8 compass points (cardinals + diagonals). Ideal for angled counters. |
| `FREE_YAW` | Continuous (0.0° - 360.0°) | Matches the exact continuous horizontal angle opposite the player's facing direction. |

### Visual Synchronization and Persistence
* **Base Model Display**: For PacketEvents virtual 3D models, the station's placement yaw is factored directly into the model's rotation quaternion transformation.
* **Child Ingredient Translation**: Local `[x, y, z]` offsets of nested process item displays are dynamically rotated around the station yaw so cooking ingredients remain aligned within the vessel.
* **Tile State Synchronization**: When using `minecraft:player_head`, the target skull block in the world is synchronized with the profile and rotation.
* **Persistent Storage**: Station yaw is serialized into `StationBinaryCodec` (Schema V4), ensuring workstations face the correct direction across chunk unloads and server restarts.

---

## 3. Heat Source Stations (`heat_source`)

Stoves and combustion fireboxes declare heat generation curves instead of recipe processing layers. Here is the configuration breakdown of `plugins/Croppy/stations/stove.yml`:

```yaml
id: "croppy:stove"
name: "<gradient:#ff8800:#ff3300>Rustic Stove</gradient>"
block: "minecraft:smoker"
rotation_mode: "CARDINAL_4"
interaction: "HYBRID"
input_capacity: 1
output_capacity: 0
menu: "croppy:stove_menu"
has_display: false

properties:
  # Flags this workstation as a thermal generator
  heat_source: "true"
  
  # Ambient baseline temperature when unlit
  min_temperature: "20.0"
  
  # Maximum thermal ceiling in Celsius
  max_temperature: "350.0"
  
  # Temperature rise per tick while fuel is burning
  heating_rate: "5.0"
  
  # Temperature dissipation per tick when fire goes out
  cooling_rate: "2.0"
  
  # Speed modifier for solid fuel consumption
  fuel_burn_multiplier: "1.0"
```

---

## 4. Field Reference Table

| Field Path | Type | Default | Description |
|---|---|---|---|
| `id` | String (`Key`) | **Required** | Unique identifier (e.g., `croppy:cooking_pot`). |
| `name` | String | Key value | Formatted display title (supports MiniMessage). |
| `block` | String | `minecraft:barrel` | Minecraft material placed in the world. |
| `rotation_mode` | Enum | `CARDINAL_4` | Directional facing mode: `CARDINAL_4`, `CARDINAL_8`, or `FREE_YAW`. Alias: `rotation`. |
| `interaction` | Enum | `HYBRID` | Interaction mode: `PHYSICAL` (hand clicks), `GUI` (container), or `HYBRID` (both). |
| `input_capacity` | Integer | `1` | Max number of distinct input item stacks. |
| `output_capacity`| Integer | `1` | Max number of distinct produced output item stacks. |
| `menu` | String (`Key`) | `croppy:default_station` | Layout key matching a file in `menus/`. |
| `has_display` | Boolean | `true` | Master switch for PacketEvents virtual entity tracking. |
| `visual.process_display.enabled` | Boolean | `false` | Enables 3D item display for cooking ingredients. |
| `visual.process_display.translation` | Float Array | `[0, 0, 0]` | Vector `[x, y, z]` local offset relative to block center. |
| `visual.process_display.scale` | Float Array | `[1, 1, 1]` | Model scaling vector `[x, y, z]`. |
| `visual.process_display.view_distance` | Integer | `16` | Max block distance for client packet rendering. |
| `visual.text_display.enabled` | Boolean | `false` | Enables floating billboard text display. |
| `visual.text_display.translation` | Float Array | `[0, 1.2, 0]` | Hologram height offset above the workstation. |
| `visual.text_display.billboard` | Enum | `CENTER` | Rotation behavior: `CENTER` (faces player), `FIXED`, `VERTICAL`, `HORIZONTAL`. |
| `visual.text_display.format` | String | — | Multiline MiniMessage template with dynamic placeholders. |

---

## 5. Text Display Dynamic Placeholders

The `visual.text_display.format` template supports the following live placeholders formatted automatically during the station tick loop:

| Placeholder | Replaced Value | Example Output |
|---|---|---|
| `{recipe_name}` | Display name of the active recipe | `Tomato Soup` |
| `{progress_bar}` | Visual color-coded progress indicator | `■■■■■□□□□□` |
| `{progress_percent}` | Numeric cooking completion percentage | `65` |
| `{temperature_c}` | Surface temperature in Celsius | `115.4` |
| `{thermal_zone}` | Current thermodynamic classification | `Simmering` (Warm, Medium, Boiling, Scorching) |
| `{state}` | High-level station status | `COOKING`, `IDLE`, `OVERCOOKING` |

---

## 6. Interaction Modes

* **`PHYSICAL`**:
  * Right-clicking the station with an ingredient inserts it directly into the cooking vessel.
  * Sneak-right-clicking with an empty hand extracts the top ingredient or cooked meal.
  * No inventory GUI ever opens. Ideal for skillets, chopping boards, and outdoor campfire grills.
* **`GUI`**:
  * Right-clicking opens the associated 9xN inventory container GUI.
  * Physical clicks on the block do not manipulate items directly.
* **`HYBRID`**:
  * Normal right-click inserts items physically for quick chef gameplay.
  * Sneak-right-click opens the container GUI for comprehensive inspection and ingredient management.

### Clean Vanilla Item Stackability
When ingredients are extracted from station storage (either via physical extraction or the GUI container window), Croppy distinguishes registered custom items from vanilla Minecraft ingredients. Vanilla ingredients are returned without artificial display names or plugin PDC metadata tags, guaranteeing that extracted vanilla items stack cleanly (`ItemStack#isSimilar() == true`) with existing item stacks in player inventories.
