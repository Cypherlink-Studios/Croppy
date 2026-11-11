# Design

## Context

See [proposal.md](file:///C:/Users/antua/OneDrive/Documentos/Programming/JAVA/PLUGINS/Croppy/openspec/changes/crop-interaction-raycast-and-inspection/proposal.md) for background and motivation.

Croppy separates domain logic (`croppy-core`) from Minecraft platform adapters (`croppy-paper`). In `croppy-paper`, 3D crop models (`model_3d`) are displayed using client-side PacketEvents entities managed by `VirtualBlockDisplayTracker`. Each display consists of an `ItemDisplay` (rendering the model item) and an `Interaction` entity (intercepting player clicks). Because these entities exist solely in client memory and `VirtualBlockDisplayTracker` (not in the Bukkit server entity world), native Bukkit methods such as `player.getTargetBlockExact()` and `player.getTargetEntity()` cannot detect them.

Currently:
1. `CroppyCommands` directly invokes `player.getTargetBlockExact(5)` in `/croppy inspect`, `/croppy water`, `/croppy fertilize`, `/croppy setstage`, and `/croppy setprogress`. When aiming at a 3D crop, the ray passes through the empty space above the minimal base block (`minecraft:tripwire`), causing commands to fail with `no-crop-found`.
2. `VirtualInteractionPacketListener` receives packet `INTERACT_ENTITY` when a player right-clicks a virtual interaction entity. It currently checks declarative interactions or falls back to harvesting mature crops, ignoring `CropInspectorService.isInspectionTool()`.

## Goals / Non-Goals

**Goals:**
- Provide a centralized `CropTargetResolver` in `croppy-paper` that resolves crops targeted by a player's line of sight across virtual interaction entities, physical blocks, and supporting farmland.
- Check solid block line-of-sight occlusion so that crops behind opaque walls cannot be targeted through obstacles.
- Refactor all 5 administrative crop targeting commands (`/croppy inspect`, `/croppy water`, `/croppy fertilize`, `/croppy setstage`, `/croppy setprogress`) to use `CropTargetResolver`.
- Route right-clicks with `croppy:inspection_tool` on virtual interaction entities in `VirtualInteractionPacketListener` to display the crop status card.

**Non-Goals:**
- Modifying `croppy-api` or `croppy-core` contracts (this is strictly platform-level raytracing and packet listener logic).
- Persisting virtual interaction entities in world chunk NBT or registering them as Bukkit entities.
- Changing reach distance from standard 5.0 blocks.

## Decisions

### Decision 1: `CropTargetResolver` Architecture & Algorithm

Implement `CropTargetResolver` in `dev.darkblade.croppy.platform.paper.util` (or `service`):
```
Player Eye (Location + Direction Vector, maxDistance = 5.0)
                       |
        +--------------+--------------+
        |                             |
        v                             v
[Solid Block Raytrace]    [Nearby Virtual Displays AABB Raytrace]
  world.rayTraceBlocks       Iterate displays in same world:
  (ignorePassable=true)      1. Spatial prune: distSq <= (5.0 + 1.0)^2
        |                    2. Build BoundingBox from hitboxWidth & height
        |                    3. box.rayTrace(eye, dir, 5.0)
        v                             |
  hitDistanceBlock                    v
        |                      hitDistanceEntity
        +--------------+--------------+
                       |
                       v
         [Occlusion & Proximity Resolution]
         If hitDistanceEntity < hitDistanceBlock:
             -> Target resolved from virtual interaction entity!
         Else if block hit is registered crop:
             -> Target resolved from physical crop block!
         Else if block hit is farmland and y+1 is registered crop:
             -> Target resolved from supporting soil!
         Else:
             -> Optional.empty()
```

*Rationale*:
- **AABB Calculation**: For each `VirtualBlockDisplay`, the center is $(x + 0.5, y, z + 0.5)$. Using `hitboxWidth` and `hitboxHeight` from `Model3DDefinition` (falling back to 0.8f if unspecified), the `BoundingBox` is:
  - $X: [pos.x() + 0.5 - w/2, pos.x() + 0.5 + w/2]$
  - $Y: [pos.y(), pos.y() + h]$
  - $Z: [pos.z() + 0.5 - w/2, pos.z() + 0.5 + w/2]$
- **Occlusion Check**: Tracing solid blocks with `ignorePassableBlocks = true` ensures transparent passable blocks (tripwire, tall grass, flowers, air) do not block line of sight, while stone, wood, dirt, and other solid walls properly occlude crops positioned behind them.
- **Alternatives Considered**:
  - *Bukkit Entity Raytracing (`world.rayTraceEntities`)*: Ineffective because virtual entities are packet-only and omitted from Bukkit entity tracking.
  - *Spawning real server entities for crops*: Rejected because it causes entity bloat, chunk NBT pollution, and persistence overhead, violating Croppy's zero-ghost-entity architecture.

### Decision 2: Inspection Tool Routing in `VirtualInteractionPacketListener`

In `VirtualInteractionPacketListener.handlePlayerInteraction`:
Before delegating to declarative interaction pipelines or harvest fallbacks:
1. Inspect the player's main hand item: `CropInspectorService.isInspectionTool(itemInHand, plugin)`.
2. If `true` and the action is `INTERACT` (right-click):
   - Invoke `inspectorService.inspect(pos, player)`
   - Send the resulting component card (or localized `no-crop-found` message).
   - Terminate the interaction handling immediately (preventing harvest or damage).

*Rationale*:
Right-clicking a 3D crop with the inspection tool should behave identically to right-clicking a vanilla crop block, providing a consistent player experience without risking accidental destruction or harvesting of mature crops.

### Decision 3: Unifying Crop Targeting Commands

In `CroppyCommands`:
Replace all instances of `Block target = player.getTargetBlockExact(5);` in:
- `inspectRaycast(Source source)`
- `waterTarget(Source source)`
- `fertilizeTarget(Source source, ...)`
- `setStage(Source source, ...)`
- `setProgress(Source source, ...)`

with:
```java
Optional<CropTargetResolver.TargetResult> targetOpt = targetResolver.resolveTarget(player, 5.0);
if (targetOpt.isEmpty()) {
    sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
    return;
}
BlockPosition pos = targetOpt.get().cropPosition();
CropInstance crop = targetOpt.get().crop();
```

*Rationale*:
Eliminates duplicated `cropManager.getCropAt(pos)` / `getCropAt(pos.add(0, 1, 0))` boilerplate across commands and guarantees consistent targeting behavior throughout the entire command surface.

## Risks / Trade-offs

- **[Risk] Performance overhead during raytracing**: Tracing multiple AABBs could theoretically introduce overhead if many crops exist.
  - *Mitigation*: Spatial distance pruning (`(dx*dx + dy*dy + dz*dz) <= 36.0`) discards 99% of displays instantly before computing AABBs. In realistic farm setups, at most 5-15 displays fall within a 5-block radius. `BoundingBox.rayTrace()` runs in nanoseconds.
- **[Risk] Misalignment between client hitbox and server AABB**: If the client renders an interaction entity at different dimensions than what the server calculates.
  - *Mitigation*: Both `PacketEventsBlockRenderer` and `CropTargetResolver` use the exact same `Model3DDefinition` hitbox fields (`hitboxWidth()` and `hitboxHeight()`), guaranteeing identical geometry between client packet rendering and server raytracing.
