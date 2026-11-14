# Design: Inspection Tool Conflict Resolution and Raycast Targeting

## Context

See `proposal.md` for background and motivation. `croppy:inspection_tool` is currently defined with `material: "COMPASS"` in `items/tools/inspector.yml`. Because WorldEdit and other administration plugins use compasses as navigation wands (`//thru` and `/jumpto`), right-clicking near ground or crops triggers automatic downward teleportation ("swallowed by the earth"). Furthermore, `InspectionToolListener` only listens to `RIGHT_CLICK_BLOCK` at `EventPriority.HIGH` and evaluates only the directly clicked block position without checking supporting farmland or line of sight.

## Goals / Non-Goals

**Goals:**
- Eliminate all conflicts with WorldEdit, FAWE, and administrative navigation wands by switching the base item material to `minecraft:stick`.
- Retain exact 3D model rendering via `item-model: "croppy:inspection_tool"` in modern Minecraft 1.21.8.
- Harden `InspectionToolListener` at `EventPriority.LOWEST` covering both `RIGHT_CLICK_BLOCK` and `RIGHT_CLICK_AIR`.
- Unify crop targeting in `InspectionToolListener` using `CropTargetResolver` to support virtual interaction entities, physical blocks, and supporting farmland (`Y + 1`).

**Non-Goals:**
- Redesigning `/croppy inspect` (which already uses `CropTargetResolver`).
- Altering the resource pack model geometry in `assets/croppy/models/item/inspection_tool.json`.
- Modifying other tools (e.g. watering cans, thermometer).

## Decisions

### Decision 1: Migrate Base Material to `minecraft:stick`
- **Choice**: Change `material: "COMPASS"` to `material: "minecraft:stick"` in `items/tools/inspector.yml`.
- **Rationale**: A stick is completely inert in vanilla Minecraft (no place, consume, or block interaction behaviors) and is never registered by default as an administrative navigation tool. It matches the pattern already used by `croppy:thermometer`.
- **Alternatives Considered**:
  - `AMETHYST_SHARD`: Suggests a lens/magnifying glass, but is less common for tool recipes.
  - `CARROT_ON_A_STICK`: Legacy approach from older custom model data days; unnecessary in 1.21.8+.
  - Keeping `COMPASS`: Would remain vulnerable to external plugins that hook packets or ignore event cancellation.

### Decision 2: Listen at `EventPriority.LOWEST` and Handle Block & Air
- **Choice**: Register `InspectionToolListener.onPlayerInteract` at `EventPriority.LOWEST` and handle both `Action.RIGHT_CLICK_BLOCK` and `Action.RIGHT_CLICK_AIR`.
- **Rationale**: In Bukkit's event pipeline, `LOWEST` runs first. If the item in hand has the `croppy:inspection_tool` PDC tag, the event is immediately cancelled (`event.setCancelled(true)`), preventing lower-priority listeners from triggering vanilla or plugin-based actions.

### Decision 3: Centralize Inspection Targeting via `CropTargetResolver`
- **Choice**: Inject `CropTargetResolver` into `InspectionToolListener`.
- **Rationale**: When right-clicking, `CropTargetResolver.resolveTarget(player, 5.0)` performs accurate line-of-sight raytracing against:
  1. Virtual 3D interaction entity bounding boxes (`hitboxWidth`, `hitboxHeight`).
  2. Physical crop blocks.
  3. Supporting farmland (`farmland` block with crop at `Y + 1`).
  4. Solid obstacle occlusion checks.
- If a target is found, inspect it; if not, send localized `command.inspect.no-crop-found`.

### Decision 4: Backward Compatibility in PDC Identification
- **Choice**: Maintain `CropInspectorService.isInspectionTool()` checking `croppy:item_id` (`croppy:inspection_tool`) and legacy `croppy:tool` (`crop_inspector`).
- **Rationale**: Existing inspection tools created prior to this change will still be recognized and can inspect crops without breakage.

## Risks / Trade-offs

- **[Risk] Existing compass inspection tools in player inventories**:
  - *Mitigation*: `CropInspectorService.isInspectionTool()` is based on PDC tags, not Bukkit `Material`. Existing compass tools will still inspect crops, but running at `LOWEST` will also help reduce wand triggers. Players crafting or receiving new tools will automatically get sticks.
- **[Risk] Unintended air interaction cancellation**:
  - *Mitigation*: The listener strictly checks `CropInspectorService.isInspectionTool(item, plugin)` before executing `event.setCancelled(true)`. Standard sticks or other items are unaffected.
