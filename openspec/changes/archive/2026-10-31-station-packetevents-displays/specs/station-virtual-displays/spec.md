# Spec Delta: station-virtual-displays

## Purpose

Provides client-side packet-based virtual display entity rendering (Block Display, Item Display, Text Display) for crafting stations via PacketEvents, featuring multi-tier distance culling, templated MiniMessage status holograms, and client-interpolated thermal animations without server entity persistence.

## ADDED Requirements

### Requirement: Virtual Display Entity Projection
The system SHALL manage client-side virtual display entities for crafting stations via PacketEvents without creating or persisting entities in the server world.

#### Scenario: Spawn virtual station entities for nearby players
- **WHEN** a player moves within the configured viewing range of a registered crafting station
- **THEN** virtual display spawn and metadata packets are dispatched to the player's connection and no Bukkit entity is created in the world

#### Scenario: Destroy virtual entities on station removal or chunk unload
- **WHEN** a crafting station is broken by a player or its enclosing chunk is unloaded
- **THEN** virtual entity destruction packets are sent to all tracking players and the virtual IDs are unmapped

### Requirement: Multi-Tier Distance Culling and Level of Detail
The system SHALL organize station visual layers into discrete distance thresholds to provide Level-of-Detail (LoD) culling and prevent visual clutter.

#### Scenario: Long-range view renders base model only
- **WHEN** a player is within the base model threshold (up to 48 blocks) but outside the process item threshold
- **THEN** only the base station model virtual entity is rendered for that player

#### Scenario: Mid-range view activates process item display
- **WHEN** a player enters the process item threshold (up to 24 blocks)
- **THEN** the in-process item virtual display is spawned and synchronized for that player

#### Scenario: Short-range view activates holographic text display
- **WHEN** a player enters the interaction threshold (up to 8 blocks)
- **THEN** the floating status text display is spawned and synchronized for that player

### Requirement: Templated MiniMessage Text Holograms
The system SHALL render dynamic status holograms formatted with configurable MiniMessage templates and contextual placeholders.

#### Scenario: Format dynamic status text with placeholders
- **WHEN** a station with an active process is evaluated for text rendering
- **THEN** template tokens including `{recipe_name}`, `{recipe_display}`, `{progress_percent}`, `{progress_bar}`, `{temperature_c}`, `{thermal_zone}`, and `{status_message}` are resolved and rendered via Text Display metadata

#### Scenario: Update text display on process tick
- **WHEN** station progress, thermal zone, or status text changes
- **THEN** an updated entity metadata packet with the newly formatted MiniMessage text is dispatched to tracking players within the text threshold

### Requirement: Client-Interpolated Thermal Animations
The system SHALL dispatch transformation metadata with client interpolation duration to render smooth visual animations based on station thermal and process states.

#### Scenario: Cookware high temperature vibration
- **WHEN** an active cookware station reaches boiling or searing temperatures above 100°C
- **THEN** transformation metadata with short interpolation duration and randomized micro-offsets is dispatched to simulate boiling vibration

#### Scenario: Process item smooth rotation
- **WHEN** an active recipe is progressing in a station
- **THEN** rotational transformation metadata with interpolation is dispatched to rotate the in-process item smoothly without per-tick packet spam
