# Proposal

## Why

In Croppy, custom crops with 3D models (`model_3d`) project virtual `ItemDisplay` and `Interaction` entities via PacketEvents packets. Their underlying physical Minecraft blocks (such as `minecraft:tripwire`) have minimal (~0.15 height) or passable hitboxes. 

Currently, administrative crop targeting commands (`/croppy inspect`, `/croppy water`, `/croppy fertilize`, `/croppy setstage`, `/croppy setprogress`) exclusively rely on Bukkit's `player.getTargetBlockExact(5)`, which only tests solid vanilla block collision shapes and is blind to virtual interaction entities. As a result, players aiming directly at the visible 3D crop body miss the tiny physical base block, causing commands to fail with `no-crop-found` unless aiming precisely at the ground string. Additionally, right-clicking a crop's virtual interaction entity while holding `croppy:inspection_tool` fails to inspect (triggering a harvest attempt if mature instead).

Unifying line-of-sight targeting against virtual interaction entity hitboxes, physical blocks, and supporting soil resolves these UX friction points and makes in-game crop inspection and management fluid and intuitive.

## What Changes

- **Unified Crop Line-of-Sight Raytracing**: Introduce a centralized targeting resolver (`CropTargetResolver`) in `croppy-paper` that computes ray intersections against active `VirtualBlockDisplay` interaction hitboxes (`BoundingBox` derived from `hitboxWidth` and `hitboxHeight`), solid block occlusions, physical crop blocks, and supporting soil (`farmland` at `y + 1`).
- **Command Targeting Modernization**: Refactor `/croppy inspect`, `/croppy water`, `/croppy fertilize`, `/croppy setstage`, and `/croppy setprogress` to use the unified target resolver instead of `player.getTargetBlockExact(5)`.
- **Inspection Tool Virtual Entity Interaction**: Update `VirtualInteractionPacketListener` so that right-clicking a virtual `Interaction` entity while holding `croppy:inspection_tool` displays the crop status card via `CropInspectorService`, preventing accidental harvest or dropped interactions.

## Capabilities

### New Capabilities

*(None. This change enhances and modifies existing crop command targeting and 3D display interaction behaviors.)*

### Modified Capabilities

- `crop-commands`: Update line-of-sight targeting for `/croppy inspect`, `/croppy water`, `/croppy fertilize`, `/croppy setstage`, and `/croppy setprogress` to resolve targets against virtual interaction entity hitboxes, physical crop blocks, and supporting soil (`y + 1`). Update the interactive inspection tool requirement to handle right-clicks on virtual interaction entities.
- `virtual-3d-block-displays`: Specify that virtual `Interaction` entities participate in server-side line-of-sight targeting calculations for player commands and route inspection tool interactions to the crop inspection service.

## Impact

- **Affected Code**:
  - `croppy-paper`:
    - New service/helper: `dev.darkblade.croppy.platform.paper.util.CropTargetResolver` (or within `service`/`command` package).
    - `dev.darkblade.croppy.platform.paper.command.CroppyCommands`: Replace direct `getTargetBlockExact` invocations with target resolver calls across crop commands.
    - `dev.darkblade.croppy.platform.paper.display.VirtualInteractionPacketListener`: Add handling for `CropInspectorService.isInspectionTool(item, plugin)`.
    - Unit test suites covering raycasting math, occlusion, and interaction handling.
- **Dependencies & Performance**: Zero new third-party dependencies. Spatial distance pruning ensures raytracing runs in $O(k)$ where $k \le 5$ nearby active displays, causing negligible tick overhead.
- **Breaking Changes**: None. Backwards compatible with existing commands, permissions, and localization keys.
