# Design: Station Visual Feedback & PacketEvents Virtual Displays

## Context

Croppy uses a strict Hexagonal Architecture across three Gradle modules (`croppy-api`, `croppy-core`, `croppy-paper`). Currently, `PaperDisplaySynchronizer` invokes `world.spawn(loc, ItemDisplay.class, ...)` to render floating items for cookware stations. This causes server-side entity persistence in chunk NBT, potential ghost entities upon ungraceful shutdown or unmonitored block breaks, and lacks support for multi-layered visual compositions (base models, ingredients, text holograms, and thermal animations).

See `proposal.md` for motivation and `specs/` for behavioral requirements.

## Goals / Non-Goals

**Goals:**
- Eliminate world entity persistence and ghost entity bugs by shifting station visual display rendering completely to virtual client-side packets using PacketEvents.
- Provide a clean, declarative YAML configuration structure (`visual:`) for crafting stations supporting three discrete visual tiers:
  1. Base 3D Model Display (Block Display or Item Display)
  2. In-Process Item Display (dynamic translation, rotation, scale, and thermal jitter)
  3. Floating Status Text Hologram (templated MiniMessage with placeholders)
- Implement a Level-of-Detail (LoD) distance culling manager that selectively sends entity packets according to player proximity.
- Maintain strict hexagonal architecture: zero PacketEvents or Bukkit imports in `croppy-api` and `croppy-core`.

**Non-Goals:**
- Shading or bundling PacketEvents into the plugin jar (PacketEvents is a required external server plugin).
- Replacing the physical Minecraft block with an invisible barrier by default (stations maintain interactive textured blocks like cauldron, smoker, barrel, or player heads).
- Client-side packet interception or custom protocol encryption.

## Decisions

### 1. PacketEvents Integration as a Required External Server Plugin
- **Decision**: Configure `compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")` in `croppy-paper` and declare `packetevents` under `depend:` in `paper-plugin.yml`.
- **Rationale**: Keeps the Croppy plugin jar lightweight, avoids classloader/netty pipeline conflicts from nested netty injectors, and allows server administrators to update PacketEvents independently across minor Minecraft releases.
- **Alternatives Considered**:
  - *Shading PacketEvents*: Adds ~5MB to artifact and requires lifecycle bootstrapping hooks in early plugin initialization (`onLoad`).
  - *Bukkit Display Entities with persistent tracking tags*: Retains chunk persistence, does not solve ghost entities on abrupt crashes, and burdens the Paper entity tracker.

### 2. Virtual Entity ID Generation via Atomic Negative Integers
- **Decision**: Allocate virtual entity IDs using an `AtomicInteger` initialized to `-1000` (e.g. `nextVirtualEntityId() -> id < 0`).
- **Rationale**: Paper and vanilla Minecraft allocate positive sequential entity IDs. Using negative integers guarantees that virtual entities never collide with live server entities, preventing entity tracker or Bukkit lookup collisions.
- **Alternatives Considered**:
  - *Spigot reflection for server entity counter*: Fragile across Paper builds and risk of desync with vanilla ticking entities.

### 3. Multi-Tier Spatial Tracking & LoD Culling
- **Decision**: Implement `StationViewerTracker` running a periodic spatial check (e.g. every 5 to 10 ticks in `PaperStationTicker` or dedicated visual ticker) evaluating player distances against three discrete radius thresholds:
  - Base Model Tier: up to 48 blocks
  - Process Item Tier: up to 24 blocks
  - Text Hologram Tier: up to 8 blocks
- **Rationale**: Sending floating holograms to players 40 blocks away wastes client and network resources and causes visual clutter. LoD thresholds ensure players only see high-frequency text details when close enough to interact.
- **Alternatives Considered**:
  - *Bukkit `PlayerMoveEvent`*: Firing distance math on every player move event is computationally expensive.
  - *Uniform render distance*: Sends all 3 entities at once, causing holographic text clutter across villages/farms.

### 4. Client-Side Interpolated Animations for Process & Thermal States
- **Decision**: Set `interpolation_duration` (e.g. 2 to 5 ticks) in `WrapperPlayServerEntityMetadata` transformations:
  - Smooth continuous Y-rotation when a recipe is running.
  - Thermal jitter: when cookware temperature exceeds boiling thresholds (e.g. 100°C), introduce randomized micro-offsets (`±0.02` in X/Z) with short 2-tick interpolation, making the item vibrate naturally in the pot/pan.
- **Rationale**: Minecraft 1.19.4+ display entities perform client-side GPU interpolation. The server sends packets only when state shifts or every few seconds, while the client renders 60+ FPS motion.
- **Alternatives Considered**:
  - *Server-side packet spam per tick (20 Hz)*: Saturates bandwidth and causes rubberbanding.

### 5. Hexagonal Component Structure
- **croppy-api**:
  - `StationVisualDefinition`: Record defining base model, process display, and text display configs.
  - `DisplayLayerConfig`: Offsets, scales, rotations, view distances, and billboard modes.
  - `TextDisplayTemplate`: Template format string, background color, line width.
  - Extend `DisplayComponent`: Expose recipe progress fraction `float` (0.0 to 1.0) and visual thermal zone.
- **croppy-core**:
  - `YamlStationLoader`: Parses `visual:` mapping in YAML.
  - Evaluates `{recipe_name}`, `{recipe_display}`, `{progress_bar}`, `{progress_percent}`, `{temperature_c}`, `{thermal_zone}`, `{status_message}` placeholders into formatted strings.
- **croppy-paper**:
  - `PacketStationDisplay`: State container storing virtual entity IDs and tracking player UUID sets for a station.
  - `PacketEventsStationRenderer`: Constructs `WrapperPlayServerSpawnEntity`, `WrapperPlayServerEntityMetadata`, and `WrapperPlayServerDestroyEntities` packets.
  - `StationViewerTracker`: Handles proximity scanning, player join/quit, and chunk load/unload cleanup.

## Risks / Trade-offs

- **[Risk] Missing PacketEvents plugin on server startup** → **Mitigation**: Paper's plugin loader automatically disables Croppy and emits a clean missing-dependency error if PacketEvents is absent, as specified in `paper-plugin.yml`.
- **[Risk] Packet overhead from frequent station updates** → **Mitigation**: Dirty checking (`DisplayComponent.isDirty()`) ensures metadata packets are only dispatched when recipes change state, progress increments significantly (e.g. >= 2%), or thermal zones transition.
- **[Risk] Ghost virtual entities if player teleports or world unloads** → **Mitigation**: Listen to `PlayerQuitEvent`, `PlayerTeleportEvent`, `PlayerChangedWorldEvent`, and `ChunkUnloadEvent` to dispatch `DestroyEntities` packets and clear tracker state.

## Migration Plan

1. Existing station definitions omitting the `visual:` YAML section continue to work seamlessly (defaults to non-visual or fallback behavior).
2. Existing real Bukkit `ItemDisplay` entities previously spawned by old versions will be cleaned up by a migration/pruning check on chunk load or server startup.
