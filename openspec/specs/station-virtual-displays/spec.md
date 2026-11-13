# station-virtual-displays Specification

## Purpose
Provides client-side packet-based virtual display entity rendering (Block Display, Item Display, Text Display) for crafting stations via PacketEvents, featuring multi-tier distance culling, templated MiniMessage status holograms, and client-interpolated thermal animations without server entity persistence.

## Requirements

### Requirement: Virtual Display Entity Projection
The system SHALL manage client-side virtual display entities for crafting stations via PacketEvents without creating or persisting entities in the server world, orienting base model rotations and spatial layer offsets according to the station's dynamic placement yaw.

#### Scenario: Spawn virtual station entities for nearby players
- **WHEN** a player moves within the configured viewing range of a registered crafting station
- **THEN** virtual display spawn and metadata packets are dispatched to the player's connection and no Bukkit entity is created in the world

#### Scenario: Spawn base model aligned with station placement yaw
- **WHEN** a virtual station base model display is spawned for a tracking player
- **THEN** its left-rotation quaternion transformation incorporates the station instance's horizontal placement yaw combined with any configured base transform rotation

#### Scenario: Destroy virtual entities on station removal or chunk unload
- **WHEN** a crafting station is broken by a player or its enclosing chunk is unloaded
- **THEN** virtual entity destruction packets are sent to all tracking players and the virtual IDs are unmapped

#### Scenario: Resolve station base model item via item registry
- **WHEN** a station declares an item-based base model visual layer
- **THEN** the base model item key is resolved against the domain ItemRegistry to construct a configured ItemStack preserving custom item-model metadata

### Requirement: Multi-Tier Distance Culling and Level of Detail
The system SHALL organize station visual layers into discrete distance thresholds to provide Level-of-Detail (LoD) culling and prevent visual clutter, rendering process item displays when items are stored in inputs or when a recipe is progressing, and culling text displays when idle if configured.

#### Scenario: Long-range view renders base model only
- **WHEN** a player is within the base model threshold (up to 48 blocks) but outside the process item threshold
- **THEN** only the base station model virtual entity is rendered for that player

#### Scenario: Mid-range view activates process item display
- **WHEN** a player enters the process item threshold (up to 24 blocks) and the station has stored inputs or an active process
- **THEN** the item virtual display is spawned and synchronized for that player displaying the appropriate item model

#### Scenario: Short-range view activates holographic text display
- **WHEN** a player enters the interaction threshold (up to 8 blocks) and the station is active, heated, holding items, or configured with `hide_when_idle: false`
- **THEN** the floating status text display is spawned and synchronized for that player

#### Scenario: Idle cold station dynamically culls text display
- **WHEN** a station is configured with `hide_when_idle: true` and is cold (ambient temperature), contains no input/output items, and has no active process
- **THEN** the virtual Text Display entity is destroyed for tracking players within the interaction threshold until heat or items are introduced

### Requirement: Templated MiniMessage Text Holograms
The system SHALL render dynamic status holograms formatted with configurable MiniMessage templates and contextual placeholders, adapting content across idle, active, paused, and completed states.

#### Scenario: Format dynamic status text with placeholders
- **WHEN** a station with an active process is evaluated for text rendering
- **THEN** template tokens including `{recipe_name}`, `{recipe_display}`, `{progress_percent}`, `{progress_bar}`, `{temperature_c}`, `{thermal_zone}`, and `{status_message}` are resolved and rendered via Text Display metadata

#### Scenario: Update text display on process tick
- **WHEN** station progress, thermal zone, or status text changes
- **THEN** an updated entity metadata packet with the newly formatted MiniMessage text is dispatched to tracking players within the text threshold

#### Scenario: Update text display on thermal convergence
- **WHEN** station temperature changes by 0.5°C or more or transitions into a new thermal zone while idle or cooking
- **THEN** the text display is marked dirty and updated metadata is dispatched to tracking players

#### Scenario: Hologram billboard orientation invariant to station placement yaw
- **WHEN** a floating text display hologram with camera tracking (e.g., CENTER or VERTICAL billboard) is rendered for a station with non-zero placement yaw
- **THEN** the text display transformation does not inherit the station instance's horizontal placement yaw in its left-rotation quaternion or translation offsets, maintaining a level camera-facing orientation

#### Scenario: Packet caching prevents redundant metadata dispatches
- **WHEN** a station display is evaluated for synchronization but the serialized rendered text component matches the previous cycle's text
- **THEN** redundant entity metadata packets are suppressed to conserve client and network bandwidth

### Requirement: Client-Interpolated Thermal Animations
The system SHALL dispatch transformation metadata with client interpolation duration to render smooth visual animations based on station thermal and process states.

#### Scenario: Cookware high temperature vibration
- **WHEN** an active cookware station reaches boiling or searing temperatures above 100°C
- **THEN** transformation metadata with short interpolation duration and randomized micro-offsets is dispatched to simulate boiling vibration

#### Scenario: Process item smooth rotation
- **WHEN** an active recipe is progressing in a station
- **THEN** rotational transformation metadata with interpolation is dispatched to rotate the in-process item smoothly without per-tick packet spam

### Requirement: Base Block Client Masking
The system SHALL mask the physical station base block with a client-side packet block change for viewing players when `hide_base_block` is enabled in the station visual configuration, re-asserting the mask across chunk transmissions and player connections.

#### Scenario: Conceal physical base block when player enters viewing range
- **WHEN** a player enters the viewing range of a station configured with `hide_base_block: true`
- **THEN** a block change packet representing an invisible barrier or air is dispatched to the player's connection while the server world retains the registered base block

#### Scenario: Conceal physical base block when player receives chunk packet
- **WHEN** a player receives a chunk packet (`PlayerChunkLoadEvent`) containing one or more registered stations configured with `hide_base_block: true`
- **THEN** client block change packets masking each station base block as an invisible barrier are dispatched immediately to the player's connection, preventing the chunk's physical block from rendering

#### Scenario: Project station displays immediately upon player login
- **WHEN** a player joins the server and is within viewing range of a registered crafting station
- **THEN** station virtual display entities and base block masking are evaluated and projected immediately without waiting for the recurring ticker cycle

#### Scenario: Restore real block appearance when station is broken or unregistered
- **WHEN** a station with base block masking is broken or removed from the world
- **THEN** virtual display entities are destroyed and block state updates reflect the true world state for tracking players

### Requirement: Virtual Interaction Entity and Packet Routing
The system SHALL project a client-side virtual `INTERACTION` entity for stations configured with an interaction hitbox, capturing player click packets and routing them to station destruction or gameplay interactions.

#### Scenario: Project virtual interaction hitbox for station viewers
- **WHEN** a player tracks a station configured with visual interaction dimensions
- **THEN** an `INTERACTION` entity is spawned at the station position with the configured width and height bounding box

#### Scenario: Route attack packet to station dismantling
- **WHEN** an inbound `INTERACT_ENTITY` packet with `ATTACK` action targets a station interaction entity ID
- **THEN** the station is dismantled on the main thread, dropping the station item and any stored inputs/outputs with break effects

#### Scenario: Route interact packet to station interaction pipeline
- **WHEN** an inbound `INTERACT_ENTITY` packet with `INTERACT` action targets a station interaction entity ID
- **THEN** the interaction is forwarded on the main thread to the declarative interaction router or station GUI and cooking handlers

### Requirement: Hot Base Model and Hitbox State Metadata Synchronization
The system SHALL dynamically synchronize workstation base model items, transformations, interaction hitbox dimensions, and ambient sensory effects via packet metadata when a station transitions between states without respawning or despawning virtual entities.

#### Scenario: Update base model item display metadata on state change
- **WHEN** a workstation instance transitions to a new active state defining an item_model or custom_model_data override
- **THEN** entity metadata packets updating the Item Display item and transform are dispatched to all tracking players without destroying or respawning the virtual display entity

#### Scenario: Update interaction hitbox dimensions on state change
- **WHEN** a workstation transitions to an active state specifying custom hitbox width and height
- **THEN** entity metadata packets updating the interaction entity dimensions are dispatched to tracking players

#### Scenario: Emit ambient state sensory effects during station ticking
- **WHEN** an active station state defines ambient particle or sound effects and the workstation ticks
- **THEN** configured particles are spawned and periodic sound loops are played at the workstation position

### Requirement: Dynamic Vertical Headroom Detection and Layout Adaptation
The system SHALL measure vertical clearance (`display.headroom`) above crafting stations using world collision bounding boxes, evaluate `DISPLAY_LAYOUT` rules, and adapt Text Display translation, scale, and formatting to fit within constrained spaces.

#### Scenario: Measure vertical headroom from world collision shapes
- **WHEN** a crafting station visual layout is evaluated
- **THEN** the system calculates vertical clearance between the top of the station model and the lowest collision boundary of blocks at `Y+1` and exposes `display.headroom` to the rule evaluation context

#### Scenario: Fit text display within available clearance
- **WHEN** an evaluated `DISPLAY_LAYOUT` rule specifies a `TEXT_DISPLAY_FIT` action
- **THEN** the Text Display vertical translation is dynamically adjusted to center the hologram within the available clearance gap without clipping into ceiling blocks

#### Scenario: Scale text display and apply compact formatting
- **WHEN** an evaluated `DISPLAY_LAYOUT` rule specifies a `TEXT_DISPLAY_SCALE` action
- **THEN** transformation metadata updates the Text Display scale vector and formats text using the rule's compact template or standard format

#### Scenario: Cache headroom and invalidate on block change
- **WHEN** the physical block above a station (`pos.y() + 1`) is placed, broken, moved by pistons, or altered
- **THEN** the cached headroom value is invalidated and recomputed on the next visual update cycle

### Requirement: Critical Clearance Action Bar Telemetry Fallback
The system SHALL suppress virtual Text Display entities when vertical headroom is critically constrained and redirect station telemetry to nearby player Action Bars.

#### Scenario: Suppress text display on critical clearance
- **WHEN** a station evaluates a `DISPLAY_LAYOUT` rule specifying `TEXT_DISPLAY_FALLBACK` with target `ACTION_BAR`
- **THEN** any existing virtual Text Display entity is destroyed for tracking players and spawn packets are withheld

#### Scenario: Dispatch Action Bar telemetry on proximity view cone look
- **WHEN** a player within configured maximum distance looks toward a station with an active Action Bar fallback within the configured view-cone angle
- **THEN** station telemetry is formatted onto the player's Action Bar, respecting a priority cooldown if recent messages were delivered

#### Scenario: Dispatch Action Bar telemetry on station interaction
- **WHEN** a player interacts with a station configured with an active Action Bar fallback
- **THEN** the station status, progress, and thermal telemetry are dispatched immediately to the player's Action Bar

