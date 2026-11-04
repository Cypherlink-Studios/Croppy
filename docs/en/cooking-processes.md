---
title: Sequential Cooking Processes
description: Technical specification and YAML guide for multi-phase cooking processes, linear durations, sensory feedback, and dynamic holograms.
sidebar:
  order: 14
---

# Sequential Cooking Processes

Croppy v0.5.0 introduces **Sequential Cooking Processes**, a decoupled state-machine engine that elevates culinary preparation from simple timers into interactive, multi-sensory cooking journeys.

While **Recipes** (`plugins/Croppy/recipes/`) define *what* ingredients turn into *which* final dish, **Processes** (`plugins/Croppy/processes/`) govern *how* that transformation occurs: cooking durations, thermal boundaries, multi-phase states, acoustic sound loops, visual particles, and floating billboard hologram displays.

---

## 1. Process Architecture & Lifecycle

Every cooking workstation running a process operates a state machine that transitions through distinct runtime states:

```text
+-----------------------------------------------------------------------------+
|                          PROCESS STATE MACHINE                              |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [IDLE]             Station awaits ingredients and valid start conditions.  |
|     │                                                                       |
|     ▼ (Ignite Button 'B' or Auto-Start)                                     |
|  [RUNNING]          Active phase: advancing ticks, particles & sound loops. |
|     │                                                                       |
|     ├─► Thermal Drop ──► [PAUSED / HOLD] ──► Temperature Restored ──► (Resume)|
|     │                                                                       |
|     ▼ (If Phase requires interaction)                                       |
|  [AWAITING_INPUT]   Waiting for player interaction (stirring, adding tool). |
|     │                                                                       |
|     ▼ (All phases complete)                                                 |
|  [COMPLETED]        Dispatches output dishes to workstation output slots.   |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### Key Capabilities
* **Linear Time Progression**: Precise tick-by-tick simulation decoupled from Minecraft random ticks.
* **Thermal Hold / Pausing (`pause_on_condition_failure: true`)**: If the heat source cools down or a campfire goes out mid-cooking, progress freezes safely without spoiling the dish.
* **Multi-Sensory Immersion**: Native PacketEvents particle spawns and rhythmic sound playback during each phase.
* **Billboard Hologram Display**: Real-time progress bar, percentage, elapsed/remaining time, and active temperature rendered via virtual Text Displays.

---

## 2. Complete YAML Schema Breakdown

Process definitions are saved as individual YAML files inside the `plugins/Croppy/processes/` directory.

Here is an annotated breakdown of `plugins/Croppy/processes/tomato_soup_cook.yml`:

```yaml
# Unique namespaced process identifier (<namespace>:<value>)
id: "croppy:tomato_soup_cook"

# Display name of the process
name: "Tomato Soup Cooking"

# Target workstation definition associated with this process
station: "croppy:cooking_pot"

# Prerequisites required before the process can ignite
start_conditions:
  - "station.temperature >= 75"

# Sequential progression phases
phases:
  - id: "simmer"
    name: "Simmering"
    type: TIMED
    duration: 15s
    
    # Condition that must hold true for progress to advance
    condition: "station.temperature >= 75"
    
    # If false, progress freezes rather than cancelling the process
    pause_on_condition_failure: true
    
    # Client-side floating hologram configuration
    display:
      template: "<gray>Simmering: <yellow>{progress_percent}%</yellow> <dark_gray>[{progress_bar}]</dark_gray> <aqua>{progress_seconds_left}s</aqua></gray>"
      bar:
        length: 10
        filled: "<gradient:#ff5555:#ffaa00>■</gradient>"
        empty: "<dark_gray>■</dark_gray>"
        
    # Particle and acoustic feedback emitted while active
    effects:
      particles:
        particle: "SPLASH"
        count: 2
        offset: [0.15, 0.05, 0.15]
        speed: 0.02
      sound:
        sound: "BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE"
        interval: 2s
        volume: 0.4
        pitch: 1.2
```

---

## 3. Field Reference Table

### Root Process Attributes

| Field Path | Type | Required | Default | Description |
|---|---|---|---|---|
| `id` | String (`Key`) | **Yes** | — | Unique process identifier (e.g., `croppy:tomato_soup_cook`). |
| `name` | String | No | ID value | Human-readable name used in diagnostic logs and fallback titles. |
| `station` | String (`Key`) | No | `croppy:default_station` | Station key restricted to running this process. |
| `start_conditions` | String or List | No | Always True | Expression(s) that must evaluate to `true` to begin the process. |
| `phases` | List of Maps | **Yes** | 1 default phase | Ordered list of progression stages. |
| `properties` | Map<String, String> | No | `{}` | Extensible custom metadata. |

### Phase Definition Attributes

| Field Path | Type | Required | Default | Description |
|---|---|---|---|---|
| `id` | String | **Yes** | — | Unique identifier for the phase within the process. |
| `name` | String | No | Capitalized ID | Phase name available via `{phase_name}` placeholder. |
| `type` | String | No | `TIMED` | Phase type: `TIMED`, `MANUAL_INTERACTION`, or `ENVIRONMENTAL`. |
| `duration` | String / Number | No | `200t` (`10s`) | Duration of the phase. Supports `15s`, `300t`, `2m`, `500ms`. |
| `timeout` | String / Number | No | `0t` (none) | Maximum duration before failure in interactive phases. |
| `condition` | String or List | No | Always True | Expression evaluated every tick; if false, triggers hold or failure. |
| `pause_on_condition_failure`| Boolean | No | `true` | When `true`, progress freezes when conditions fail instead of aborting. |
| `failure_policy` | String | No | `CANCEL` | Action on failure: `CANCEL`, `SPOIL`, or `BYPRODUCT`. |
| `byproduct` | String (`Key`) | No | — | Item identifier produced if the phase fails or spoils. |
| `prompt` | String | No | — | Message or action bar prompt displayed during manual interaction. |
| `display.template` | String | No | Default Template| MiniMessage template string rendered on the floating hologram. |
| `display.bar.length` | Integer | No | `10` | Total symbol count of the `{progress_bar}`. |
| `display.bar.filled` | String | No | `<green>■</green>` | MiniMessage symbol representing completed progress. |
| `display.bar.empty` | String | No | `<dark_gray>■</dark_gray>`| MiniMessage symbol representing remaining progress. |
| `effects.particles` | Map | No | None | Particle type, count, offset `[x, y, z]`, and speed. |
| `effects.sound` | Map | No | None | Bukkit sound name, interval (`2s`, `40t`), volume, and pitch. |

---

## 4. Duration Formats & Time Units

Process durations and sound intervals support human-friendly time suffixes:

| Suffix | Unit | Equivalent Ticks | Example |
|---|---|---|---|
| `s` | Seconds | `value * 20` ticks | `10s` (200 ticks) |
| `t` | Server Ticks | `value` ticks | `300t` (15 seconds) |
| `m` | Minutes | `value * 1200` ticks | `1.5m` (1800 ticks) |
| `ms` | Milliseconds | `(value / 50)` ticks | `1500ms` (30 ticks) |
| *(None)* | Numeric Ticks | Direct integer value | `200` (200 ticks) |

---

## 5. Thermal Hold & Condition Pausing

Traditional cooking mods cancel the entire recipe if the stove runs out of fuel for even a fraction of a second. Croppy's **Condition Hold Engine** solves this:

When `pause_on_condition_failure: true` is configured on a phase:
1. If the station temperature drops below the required threshold (e.g., `station.temperature >= 75`), the phase transitions into a **STALLED / HOLD** state.
2. The active progress percentage and elapsed seconds are preserved intact in memory and saved to disk.
3. Particle loops and sound effects pause to visually signal that heat has stopped.
4. As soon as the player fuels the stove or re-ignites the campfire, heat rises above `75°C` and the process immediately resumes from where it paused.

---

## 6. Dynamic Hologram Placeholders

During active cooking, the station's virtual Text Display renders real-time information evaluated via `ProcessDisplayFormatter`. The following placeholders can be used within `display.template`:

| Placeholder | Output Example | Description |
|---|---|---|
| `{progress_percent}` | `45` | Elapsed phase progress percentage (`0` to `100`). |
| `{progress_percent_left}` | `55` | Inverted remaining percentage (`100` to `0`). |
| `{progress_seconds}` | `6` | Number of seconds elapsed in the current phase. |
| `{progress_seconds_left}` | `9` | Number of seconds remaining until the phase completes. |
| `{progress_ticks}` | `134` | Exact server ticks elapsed in the current phase. |
| `{progress_ticks_left}` | `166` | Exact server ticks remaining in the current phase. |
| `{progress_bar}` | `■■■■□□□□□□` | Dynamic visual progress bar styled with `bar.filled` and `bar.empty`. |
| `{phase_name}` | `Simmering` | Name of the active phase. |
| `{phase_index}` | `1` | 1-based sequential index of the active phase. |
| `{phase_total}` | `3` | Total number of phases in the process. |
| `{recipe_name}` | `Tomato Soup` | Display name of the culinary recipe currently being prepared. |
| `{temperature}` | `82.4` | Formatted current temperature of the cooking station (°C). |

---

## 7. Multi-Sensory Feedback (Particles & Audio)

To deliver an authentic cooking atmosphere, phases support continuous particle emissions and rhythmic audio cues:

### Particle Configuration
```yaml
effects:
  particles:
    particle: "CAMPFIRE_COSY_SMOKE" # Any valid Minecraft particle type
    count: 3                        # Number of particles per tick cycle
    offset: [0.15, 0.08, 0.15]      # Spread along X, Y, Z axes
    speed: 0.01                     # Particle initial velocity
```

### Sound Configuration
```yaml
effects:
  sound:
    sound: "BLOCK_FIRE_EXTINGUISH"  # Bukkit Sound enum or namespaced key
    interval: 30t                   # Sound replay interval (supports s, t, m)
    volume: 0.35                    # Audio volume (0.0 to 1.0+)
    pitch: 1.4                      # Audio pitch (0.5 to 2.0)
```

---

## 8. Step-by-Step Tutorial: Multi-Phase Reduction Sauce

Let's configure a sophisticated multi-phase process: **Balsamic Tomato Reduction** (`croppy:balsamic_reduction`).

### Step 1: Create the Process Definition
Create `plugins/Croppy/processes/balsamic_reduction.yml`:

```yaml
id: "croppy:balsamic_reduction"
name: "Balsamic Tomato Reduction"
station: "croppy:cooking_pot"

start_conditions:
  - "station.temperature >= 90"

phases:
  # Phase 1: High Heat Flash Boil (10 seconds)
  - id: "boil"
    name: "Flash Boiling"
    type: TIMED
    duration: 10s
    condition: "station.temperature >= 90"
    pause_on_condition_failure: true
    display:
      template: "<red>Boiling:</red> <yellow>{progress_percent}%</yellow> <dark_gray>[{progress_bar}]</dark_gray> <gray>({progress_seconds_left}s)</gray>"
      bar:
        length: 8
        filled: "<red>■</red>"
        empty: "<dark_gray>■</dark_gray>"
    effects:
      particles:
        particle: "SPLASH"
        count: 4
        offset: [0.2, 0.05, 0.2]
        speed: 0.05
      sound:
        sound: "BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE"
        interval: 1s
        volume: 0.6
        pitch: 1.5

  # Phase 2: Gentle Simmering Reduction (20 seconds)
  - id: "simmer"
    name: "Slow Simmer"
    type: TIMED
    duration: 20s
    condition: "station.temperature >= 60"
    pause_on_condition_failure: true
    display:
      template: "<gold>Simmering:</gold> <yellow>{progress_percent}%</yellow> <dark_gray>[{progress_bar}]</dark_gray> <gray>({progress_seconds_left}s)</gray>"
      bar:
        length: 8
        filled: "<gold>■</gold>"
        empty: "<dark_gray>■</dark_gray>"
    effects:
      particles:
        particle: "SMOKE"
        count: 2
        offset: [0.1, 0.1, 0.1]
        speed: 0.01
      sound:
        sound: "BLOCK_BREWING_STAND_BREW"
        interval: 3s
        volume: 0.4
        pitch: 0.9
```

### Step 2: Link the Process in a Recipe
In `plugins/Croppy/recipes/balsamic_sauce.yml`:

```yaml
id: "croppy:balsamic_sauce"
name: "<gradient:#aa0000:#550000>Balsamic Tomato Sauce</gradient>"
category: "croppy:condiments"
station: "croppy:cooking_pot"

# Link to our custom process
process: "croppy:balsamic_reduction"

inputs:
  - item: "croppy:tomato"
    amount: 3
  - item: "minecraft:sugar"
    amount: 1
  - item: "minecraft:glass_bottle"
    amount: 1

result:
  item: "croppy:balsamic_sauce"
  amount: 1
```

### Step 3: Reload & Cook
1. Run `/croppy reload`.
2. Place the ingredients in a Cooking Pot over an active heat source.
3. Open the Cooking Pot GUI and click button **'B'** (Ignite Cookware).
4. Watch the stage dynamically transition from **Flash Boiling** into **Slow Simmer** with distinctive sounds, particles, and hologram updates!
