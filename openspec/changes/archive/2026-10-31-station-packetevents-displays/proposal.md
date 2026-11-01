# Proposal: Station Visual Feedback & PacketEvents Virtual Displays

## Why

Currently, crafting stations in Croppy rely on real Bukkit `ItemDisplay` entities spawned directly into the Minecraft world (`world.spawn(loc, ItemDisplay.class, ...)`). This design suffers from three critical limitations:
1. **Ghost Entities & Chunk Persistence**: Server-side Bukkit display entities persist into chunk NBT files (`.mca`). If a server crashes, stops abruptly, or if a station block is removed without an active listener, orphan ghost entities remain permanently suspended in the world.
2. **Server-Side Overhead & Lack of Visibility Control**: Real entities occupy Paper entity tracking slots, collision and spatial query buckets, and render universally across arbitrary distances. Players at long ranges are inundated with floating items and holographic text that clutter the landscape.
3. **Limited Visual Composition**: Stations currently lack support for multi-layered visual representations (e.g. 3D station base models, distinct in-process ingredients, dynamic status holograms, and thermal boiling/searing jitter animations).

By integrating **PacketEvents** as a required external plugin dependency, Croppy can project purely virtual client-side Display Entities (Block Display, Item Display, and Text Display) directly into viewers' packet streams. This eliminates world corruption, provides zero server tick load, enables distance-based Level-of-Detail (LoD) culling, and delivers smooth, client-interpolated culinary animations.

## What Changes

- **PacketEvents Dependency**: Declare `packetevents` as a required server plugin dependency in `paper-plugin.yml` and add `compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")` in `croppy-paper`.
- **Pure Domain Visual Modeling (`croppy-api`)**: Define immutable records for station visual configurations (`StationVisualDefinition`, `DisplayLayerConfig`, `TextDisplayTemplate`, `DisplayTransform`, and `BillboardMode`). Extend `DisplayComponent` to expose process progress fractions and visual thermal states.
- **Declarative YAML Configuration (`croppy-core`)**: Update `YamlStationLoader` to parse a structured `visual:` block in station definitions supporting base models, in-process display parameters, and templated text holograms.
- **Packet-Based Virtual Display Engine (`croppy-paper`)**:
  - Implement `PacketStationDisplay` managing virtual entity IDs (allocated safely via negative integer counters) for 3 distinct visual tiers (Base Model, Process Item, Text Hologram).
  - Implement `PacketStationRenderer` converting domain visual states into PacketEvents packets (`WrapperPlayServerSpawnEntity`, `WrapperPlayServerEntityMetadata`, and `WrapperPlayServerDestroyEntities`).
  - Implement `StationViewerTracker` running a lightweight periodic spatial scan to cull or display visual layers according to 3 discrete distance thresholds (LoD).
  - Add client-side interpolated thermal jitter animations when cookware reaches boiling or searing temperatures.
  - Implement full lifecycle packet cleanup on station block break, chunk unload, and player disconnect.

## Capabilities

### New Capabilities
- `station-virtual-displays`: Covers client-side packet-driven virtual display projection (Block Display, Item Display, Text Display) for crafting stations via PacketEvents, multi-tier LoD viewer distance culling, MiniMessage placeholder formatting, and client-interpolated thermal animations.

### Modified Capabilities
- `crafting-stations`: Modifies Requirement `Display and Visual Synchronization` to specify that station visual representations are projected virtually through packet protocols rather than spawned as persistent Bukkit entities in the world, eliminating ghost entity risks and integrating LoD viewer tracking.

## Impact

- **Dependencies**: Adds compile-only dependency on `com.github.retrooper:packetevents-spigot:2.7.0` to `croppy-paper`, requiring `packetevents` on the Paper server. Zero dependencies added to `croppy-api` and `croppy-core`.
- **APIs**: New visual definition records in `dev.darkblade.croppy.api.station.visual` and backward-compatible extensions to `StationDefinition` and `DisplayComponent`.
- **Compatibility**: Retains full compatibility with existing YAML station files (omitting `visual:` gracefully defaults to standard non-packet or disabled visual state).
- **Architecture**: Inviolable rule preserved: `croppy-api` and `croppy-core` contain zero Bukkit and zero PacketEvents imports. All packet construction and viewer tracking remain encapsulated in `croppy-paper`.
