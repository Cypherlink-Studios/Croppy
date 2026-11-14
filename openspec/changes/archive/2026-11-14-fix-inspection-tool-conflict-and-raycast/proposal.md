# Proposal: Fix Inspection Tool Navigation Wand Conflict and Raycast Targeting

## Why

The interactive crop inspection tool (`croppy:inspection_tool`) currently uses `material: COMPASS`. In Minecraft Paper server environments with administrative plugins like WorldEdit or FastAsyncWorldEdit (FAWE), `COMPASS` is bound by default to the navigation wand (`//tool navigation`). When an administrator or player with navigation permissions right-clicks a crop or farmland, WorldEdit intercepts the compass interaction and executes `//thru`, teleporting the player forward through the ground and causing them to be swallowed into the earth or subterranean caves. Furthermore, `InspectionToolListener` runs at `EventPriority.HIGH` (missing pre-emptive cancellation before WorldEdit at `LOWEST`), ignores air clicks, and fails to resolve crops when clicking supporting farmland (`Y`) instead of the crop block (`Y + 1`).

## What Changes

- **Migrate Base Material to `minecraft:stick`**: Change the underlying vanilla material of `croppy:inspection_tool` from `COMPASS` to `minecraft:stick`. Because the tool uses Minecraft 1.21.4+ `item-model: "croppy:inspection_tool"`, its custom 3D model and in-game visuals remain completely identical while eliminating all navigation wand conflicts.
- **Harden `InspectionToolListener` Priority and Action Filtering**: Shift listener priority from `EventPriority.HIGH` to `EventPriority.LOWEST` and handle both `Action.RIGHT_CLICK_BLOCK` and `Action.RIGHT_CLICK_AIR`, instantly cancelling the interaction event before other plugins can interpret the item.
- **Unify Inspection Targeting with `CropTargetResolver`**: Replace naive `event.getClickedBlock()` queries with `CropTargetResolver`, allowing the inspection tool to accurately resolve virtual interaction entities, physical crop blocks, and supporting soil/farmland (`Y + 1`) within 5 blocks line-of-sight reach.
- **Keep Crafting Consistency**: Retain the existing crafting recipe shape while ensuring the result item is the new `minecraft:stick` based inspection tool.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `crop-commands`: Update the `Interactive Inspection Tool` requirement to require a conflict-free neutral base material (`minecraft:stick`) and line-of-sight raytracing targeting (resolving virtual interaction entities, physical crop blocks, and supporting farmland) on right-click interactions.

## Impact

- **Affected Files**:
  - `croppy-paper/src/main/resources/items/tools/inspector.yml`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/listener/InspectionToolListener.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/CroppyPlugin.java` (injecting `CropTargetResolver` into `InspectionToolListener`)
  - `croppy-paper/src/test/java/dev/darkblade/croppy/platform/paper/crafting/BundledCraftingRecipesIntegrityTest.java`
  - Documentation referencing item definition (`docs/en/configuration.md`, `docs/es/configuration.md`, `docs/en/crafting-recipes.md`).
- **Dependencies**: No external API or dependency changes; strictly uses Java 21 and Paper 1.21.8 APIs.
