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

Here is a fully documented example representing a complete workstation configuration with base models, process previews, holographic text, and dynamic states (`plugins/Croppy/stations/cooking_pot.yml`):

```yaml
# Unique namespaced station key
id: "croppy:cooking_pot"

# Display name with MiniMessage formatting
name: "<gradient:#ffaa00:#ff5555>Cooking Pot</gradient>"

# Physical Minecraft block backing this workstation in the world
block: "minecraft:barrier"

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
  # Hides physical block with clientbound barrier packets to avoid visual obstruction
  hide_base_block: true

  # Virtual INTERACTION entity dimensions for player hit testing
  interaction:
    width: 1.01
    height: 1.01

  # 3D Base Model Display: The default physical vessel
  base_model:
    enabled: true
    type: "ITEM"
    item: "croppy:cooking_pot"
    translation: [0.0, 0.5, 0.0]
    scale: [1.0, 1.0, 1.0]
    rotation: [0.0, 0.0, 0.0]
    billboard: "FIXED"
    view_distance: 48
    interpolation_duration: 0

  # 3D Process Display: Simmering ingredient preview
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
    hide_when_idle: true
    format: |
      <gradient:#ffaa00:#ff5555><bold>{recipe_name}</bold></gradient>
      {progress_bar} <white>{progress_percent}%</white>
      <gray>Heat: <aqua>{temperature_c}°C</aqua> (<gold>{thermal_zone}</gold>)</gray>

# Reactive Station States (evaluated dynamically by descending priority)
states:
  - id: "boiling"
    priority: 30
    when: "temperature >= 100.0 && process_state == 'RUNNING'"
    model:
      item_model: "croppy:station/cooking_pot/boiling"
      scale: [1.02, 1.05, 1.02]
    hitbox:
      width: 1.01
      height: 1.10
    effects:
      particle:
        type: "SPLASH"
        count: 3
        offset: [0.15, 0.45, 0.15]
        speed: 0.02
      sound:
        sound: "block.bubble_column.upwards_inside"
        interval_ticks: 30
        volume: 0.6
        pitch: 1.2

  - id: "cooking"
    priority: 20
    when: "process_state == 'RUNNING'"
    model:
      item_model: "croppy:station/cooking_pot/cooking"
    effects:
      particle:
        type: "SMOKE"
        count: 2
        offset: [0.1, 0.5, 0.1]
        speed: 0.01

  - id: "has_liquid"
    priority: 10
    when: "has_inputs == true"
    model:
      item_model: "croppy:station/cooking_pot/water"

  - id: "idle"
    priority: 0
    model:
      item_model: "croppy:cooking_pot"

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

## 4. Station States & Reactive Evaluation (`states:`)

Croppy v0.6.0 introduces a declarative **Station States** system. Workstations dynamically react to their environmental and physical state (such as heat levels, fuel remaining, cooking progress, or stored inputs) by altering their 3D item model, adjusting display scale/offsets, resizing their clickable interaction hitbox, and emitting ambient sensory particles and audio loops.

### Reactive Zero-Persistence Architecture
Unlike crops which persist growth stages to chunk PDC, station states are **evaluated 100% reactively in runtime** from the workstation's physical components (`ThermalComponent`, `HeatSourceComponent`, `StorageComponent`, `ProcessComponent`). When chunks unload and reload, or when the server restarts, states are recalculated on the fly from physical properties without requiring synthetic state data.

```
[Physical Components: Temp / Fuel / Process / Storage]
                     │
                     ▼
       StationStateEvaluator.evaluate()
                     │
                     ▼ (Resolves by descending priority)
       [Active State Definition]
       ├── Model Override (item_model, cmd, translation, scale, rotation)
       ├── Hitbox Override (width, height)
       └── Phase Effects (ambient particles, periodic audio loops)
                     │
                     ▼ (Zero-Despawn Hot Metadata Packets)
       PacketEvents Clientbound Synchronization
```

### Condition Expressions & Available Variables
State transitions are governed by boolean AST condition expressions configured in `when:` (or `condition:`). During each station tick, variables are injected from the station's physical state:

| Variable | Type | Description | Example Condition |
|---|---|---|---|
| `temperature` | Double | Workstation surface temperature in Celsius. | `temperature >= 100.0` |
| `temp` | Double | Alias for `temperature`. | `temp > 250.0` |
| `thermal_zone` | String | Thermodynamic classification: `COLD`, `WARM`, `SIMMERING`, `BOILING`, `SEARING`, `BURNING`. | `thermal_zone == 'BOILING'` |
| `fuel_ticks` | Long | Remaining combustion ticks of the underlying heat source. | `fuel_ticks > 0` |
| `is_active` | Boolean | True if the station is heated (> 25°C), running a process, or has fuel. | `is_active == true` |
| `process_state` | String | State of active culinary process: `IDLE`, `RUNNING`, `AWAITING_INPUT`, `COMPLETED`, `PAUSED`. | `process_state == 'RUNNING'` |
| `has_inputs` | Boolean | True if input inventory contains at least one item stack. | `has_inputs == true` |
| `input_count` | Integer | Total number of item stacks currently stored in input slots. | `input_count >= 3` |
| `has_outputs` | Boolean | True if output inventory contains at least one completed item. | `has_outputs == true` |

States are evaluated in **descending priority order**. The first state whose condition expression evaluates to `true` becomes active. If no conditions match, the station falls back to the baseline default configuration.

---

### Model Overrides (`states[].model`)

The `model:` block allows overriding the station's base 3D display model, custom model data, and spatial orientation:

```yaml
states:
  - id: "boiling"
    priority: 30
    when: "temperature >= 100.0 && process_state == 'RUNNING'"
    model:
      # Minecraft 1.21.4+ native namespaced item model definition
      item_model: "croppy:station/cooking_pot/boiling"
      
      # Optional custom model data integer (if using legacy CMD packs)
      custom_model_data: 1005
      
      # Spatial offsets and scaling relative to block center
      translation: [0.0, 0.52, 0.0]
      scale: [1.02, 1.05, 1.02]
      rotation: [0.0, 45.0, 0.0]
```

* **Transform Coordinate Inheritance**: Any omitted transform property automatically inherits its value from `visual.base_model`. For instance, specifying only `scale: [1.1, 1.1, 1.1]` scales the station while preserving base translation and rotation.
* **Transform-Only States**: A state can specify only `scale`, `translation`, or `rotation` without declaring an `item_model`, allowing physical size or orientation animations while retaining the base item.

---

### Dynamic Hitbox Overrides (`states[].hitbox`)

The `hitbox:` block resizes the client-side virtual `INTERACTION` bounding box to match the station's physical state:

```yaml
states:
  - id: "boiling"
    hitbox:
      width: 1.05
      height: 1.10
```

* **Flexible Syntax**:
  * Map format: `hitbox: { width: 1.05, height: 1.10 }` (omitted dimensions inherit from `visual.interaction`).
  * Array format: `hitbox: [1.05, 1.10]`.
  * Uniform scalar: `hitbox: 1.05` (applies to both width and height).
* **Zero-Despawn Resizing**: When a station transitions between states, PacketEvents transmits metadata updates (`INDEX_INTERACTION_WIDTH` and `INDEX_INTERACTION_HEIGHT`) without destroying or recreating the virtual entity.

---

### Ambient Sensory Effects (`states[].effects`)

Active states can emit ambient particles and periodic looping audio:

```yaml
states:
  - id: "boiling"
    effects:
      particle:
        type: "SPLASH"
        count: 3
        offset: [0.15, 0.45, 0.15]
        speed: 0.02
      sound:
        sound: "block.bubble_column.upwards_inside"
        interval_ticks: 30
        volume: 0.6
        pitch: 1.2
```

* **Particles**: Supported particle types match Bukkit `Particle` enum names (e.g., `CAMPFIRE_COSY_SMOKE`, `SPLASH`, `SMOKE`, `FLAME`).
* **Audio Loops**: Plays the specified Minecraft sound event at the configured tick interval (e.g., `interval_ticks: 30` = every 1.5 seconds).

---

## 5. Field Reference Table

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
| `visual.hide_base_block` | Boolean | `false` | Masks physical base block with clientbound barrier packets to avoid visual interference with 3D displays. |
| `visual.interaction.width` | Float | `0.8` | Baseline bounding box width for the virtual `INTERACTION` entity. |
| `visual.interaction.height` | Float | `0.25` | Baseline bounding box height for the virtual `INTERACTION` entity. |
| `visual.base_model.enabled` | Boolean | `true` | Enables baseline 3D display for the station vessel. |
| `visual.base_model.type` | String | `ITEM` | Display entity type: `ITEM` or `BLOCK`. |
| `visual.base_model.item` | String (`Key`) | `null` | Item identifier backing the Item Display (e.g., `croppy:cooking_pot`). |
| `visual.base_model.translation` | Float Array | `[0, 0, 0]` | Base spatial translation offset `[x, y, z]`. |
| `visual.base_model.scale` | Float Array | `[1, 1, 1]` | Base spatial scale factor `[x, y, z]`. |
| `visual.base_model.rotation` | Float Array | `[0, 0, 0]` | Base Euler rotation in degrees `[pitch, yaw, roll]`. |
| `visual.base_model.billboard` | Enum | `FIXED` | Display billboard behavior: `FIXED`, `CENTER`, `VERTICAL`, `HORIZONTAL`. |
| `visual.base_model.view_distance` | Integer | `48` | View distance in blocks for base model packet rendering. |
| `visual.process_display.enabled` | Boolean | `false` | Enables 3D item display for cooking ingredients. |
| `visual.process_display.translation` | Float Array | `[0, 0, 0]` | Vector `[x, y, z]` local offset relative to block center. |
| `visual.process_display.scale` | Float Array | `[1, 1, 1]` | Model scaling vector `[x, y, z]`. |
| `visual.process_display.view_distance` | Integer | `24` | Max block distance for process item rendering. |
| `visual.text_display.enabled` | Boolean | `false` | Enables floating billboard text display. |
| `visual.text_display.translation` | Float Array | `[0, 1.2, 0]` | Hologram height offset above the workstation. |
| `visual.text_display.billboard` | Enum | `CENTER` | Rotation behavior: `CENTER` (faces player), `FIXED`, `VERTICAL`, `HORIZONTAL`. |
| `visual.text_display.line_width` | Integer | `200` | Maximum line width before client-side text wrapping occurs. |
| `visual.text_display.hide_when_idle` | Boolean | `false` | Dynamically culls floating text when cold (<= 25°C), empty, and inactive. |
| `visual.text_display.format` | String | — | Multiline MiniMessage template with dynamic placeholders. |
| `states` | List | `[]` | List of prioritized declarative state definitions. |
| `states[].id` | String | **Required** | Unique state identifier (e.g., `boiling`, `lit`, `idle`). |
| `states[].priority` | Integer | `0` | Evaluation priority (higher numbers evaluated first). |
| `states[].when` | String | `null` | AST boolean condition expression (alias: `condition`). |
| `states[].model.item_model` | String (`Key`) | `null` | Minecraft 1.21.4+ namespaced item model definition. |
| `states[].model.custom_model_data` | Integer | `0` | Custom model data integer (0 if disabled). |
| `states[].model.translation` | Float Array | `base` | Spatial translation offset `[x, y, z]`. |
| `states[].model.scale` | Float Array | `base` | Scale factor `[x, y, z]` or uniform scalar. |
| `states[].model.rotation` | Float Array | `base` | Euler angles in degrees `[pitch, yaw, roll]`. |
| `states[].hitbox.width` | Float | `base` | Overridden interaction bounding box width. |
| `states[].hitbox.height` | Float | `base` | Overridden interaction bounding box height. |
| `states[].effects.particle.type` | String | `null` | Bukkit `Particle` enum name. |
| `states[].effects.particle.count` | Integer | `2` | Number of particle particles emitted per tick. |
| `states[].effects.particle.offset` | Float Array | `[0.15, 0.05, 0.15]` | Particle spawn dispersion offset `[x, y, z]`. |
| `states[].effects.particle.speed` | Float | `0.02` | Particle motion speed. |
| `states[].effects.sound.sound` | String | `null` | Minecraft sound key (e.g., `block.fire.ambient`). |
| `states[].effects.sound.interval_ticks` | Integer | `40` | Ticking interval between audio loop replays. |
| `states[].effects.sound.volume` | Float | `1.0` | Sound playback volume. |
| `states[].effects.sound.pitch` | Float | `1.0` | Sound playback pitch. |

---

## 6. Text Display Dynamic Placeholders

The `visual.text_display.format` template supports the following live placeholders formatted automatically during the station tick loop:

| Placeholder | Replaced Value | Example Output |
|---|---|---|
| `{recipe_name}` | Display name of the active recipe (or `Idle` fallback) | `Tomato Soup` |
| `{recipe_display}` | Formatted recipe display name or default fallback | `<yellow>Tomato Soup</yellow>` |
| `{progress_bar}` | Visual color-coded progress indicator | `■■■■■□□□□□` |
| `{progress_percent}` | Numeric cooking completion percentage (`0-100`) | `65` |
| `{progress_percent_left}` | Remaining completion percentage (`100-0`) | `35` |
| `{progress_seconds}` | Elapsed process time in whole seconds | `12` |
| `{progress_seconds_left}` | Remaining process countdown in seconds | `8` |
| `{progress_ticks}` | Elapsed process duration in ticks | `240` |
| `{progress_ticks_left}` | Remaining process duration in ticks | `160` |
| `{time_remaining}` | Human-readable remaining countdown string | `8s` / `1m 20s` |
| `{temperature_c}` | Surface temperature in Celsius with degree symbol | `115.4°C` |
| `{temperature}` | Plain numeric surface temperature without unit | `115.4` |
| `{thermal_zone}` | Current thermodynamic classification | `SIMMERING` (NONE, WARM, MEDIUM, BOILING, SCORCHING) |
| `{phase_name}` | Name of the active sequential process phase | `Searing` |
| `{phase_index}` | 1-based index of the current process phase | `1` |
| `{phase_total}` | Total number of phases in the sequential recipe | `3` |
| `{status_message}` | Contextual diagnostic or error feedback message | `Missing Heat Source` |
| `{state}` | High-level station status | `COOKING`, `IDLE`, `HEATING`, `PAUSED`, `COMPLETED`, `FAILED`, `OVERCOOKING` |

---

## 7. Interaction Modes

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
