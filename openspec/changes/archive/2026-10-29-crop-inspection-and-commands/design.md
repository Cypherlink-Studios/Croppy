# Design: Crop Inspection and Administration Commands

## Context
See `proposal.md` for motivation.
Croppy currently contains a clean headless API and Core with registries, lifecycle tracking, growth simulation, and hybrid chunk-backed persistence. However, server operators have no user-facing interface to observe, test, or control these systems in-game.

Paper 1.21.8 runs Minecraft modern command dispatching via Brigader / `CommandSourceStack`. The Incendo Cloud 2.0 framework (`org.incendo:cloud-paper:2.0.1` and `cloud-annotations:2.0.0`) provides type-safe, declarative command handling and tab-completion for modern Paper.

## Goals / Non-Goals

**Goals:**
- Provide intuitive administration commands under the `/croppy` namespace with robust permission handling (`croppy.admin`).
- Implement raytrace and coordinate-based crop inspection outputting rich MiniMessage diagnostics.
- Implement an interactive right-click inspection tool identified by PDC key `croppy:tool` and configured in `config.yml`.
- Support hot-reloading plugin configuration and species definitions at runtime (`/croppy reload`).
- Enable rapid debugging with `/croppy setstage` and `/croppy setprogress`.
- Enable crop item distribution via `/croppy give <player> <species> [amount]`.
- Maintain complete architectural decoupling: all Cloud 2.0 and Bukkit dependencies reside exclusively in `croppy-paper`.

**Non-Goals:**
- Visual client-side particle overlays or holographic display texts (handled in future UI/visual enhancement changes).
- Economy or shop integration for purchasing crop items.
- Modifying offline player inventories.

## Decisions

### Decision 1: Incendo Cloud 2.0.1 with PaperCommandManager<CommandSourceStack>
- **Rationale**: Paper 1.20.6+ and 1.21.x utilize Mojang's `CommandSourceStack`. Incendo Cloud 2.0.1 provides native Paper support with `PaperCommandManager.builder().executionCoordinator(ExecutionCoordinator.simpleCoordinator()).buildOnEnable(plugin)`.
- **Alternatives Considered**: 
  - *Vanilla Bukkit `CommandExecutor`*: Requires manual argument parsing, tedious tab-completion logic, and lacks modern brigadier tree mapping.
  - *Cloud 1.x / `PaperCommandManager<CommandSender>`*: Obsolete on 1.20.6+ and creates deprecation warnings with `CommandSourceStack`.

### Decision 2: Custom Species Cloud Parser with Asynchronous Tab Completion
- **Rationale**: By implementing Cloud 2.0's `ArgumentParser<CommandSourceStack, Species>`, commands can accept `Species` as a strongly-typed argument. The parser queries `SpeciesRegistry` by `Key.parse(...)` and provides suggestions based on all registered species keys.
- **Alternatives Considered**: Accepting raw strings and resolving manually inside each command method, which causes code duplication and loses native Brigadier auto-completion.

### Decision 3: MiniMessage Status Card Formatter
- **Rationale**: Kyori Adventure's MiniMessage standard delivers vibrant, modern text components. The inspection card formats species key and display name, current stage index / total stages, a visual progress bar (e.g. `[■■■■□□□□] 50%`), environmental light vs required light, water fluid level (0–8) vs requirement, and overall growth readiness.
- **Alternatives Considered**: Raw legacy color codes (`&a`, `&b`), which are deprecated and do not support hex gradients or structured hover components.

### Decision 4: PDC-Tagged Configurable Inspection Tool Item
- **Rationale**: In `config.yml`, operators configure `inspection-tool.material` (default `SPYGLASS`), `name`, and `lore`. The item is tagged with `croppy:tool` (`NamespacedKey`) storing a string marker (`crop_inspector`) in its `PersistentDataContainer`. The `InspectionToolListener` cancels right-clicks on blocks when holding this tool and triggers inspection.
- **Alternatives Considered**:
  - Matching by item display name: Fragile and easily forged by players renaming items with an anvil.
  - Fixed hardcoded item: Inflexible for servers using spyglasses or sticks for other mechanics.

### Decision 5: Non-Destructive In-Memory Hot Reloading
- **Rationale**: `/croppy reload` reloads `config.yml`, reads the configured inspection tool settings, and triggers `YamlSpeciesLoader` to scan `species/` in the plugin data folder. Newly found or modified species are re-registered in `SpeciesRegistry`. Existing crop instances in `CropManager` retain their positions and references.
- **Alternatives Considered**: Restarting the entire plugin lifecycle or unregistering events, which can leak listeners or disrupt active chunk operations.

## Risks / Trade-offs

- **[Risk]** Hot-reloading a species with fewer stages than an existing crop instance currently has.
  - *Mitigation*: When a crop's current stage index exceeds the newly reloaded species' stage count, `GrowthEngine` clamps the stage index to the maximum valid stage, preventing `IndexOutOfBoundsException`.
- **[Risk]** Player right-clicking interactable blocks (e.g. chests, farmlands, buttons) with the inspection tool.
  - *Mitigation*: The `InspectionToolListener` listens at `EventPriority.HIGH` and explicitly calls `event.setCancelled(true)` when interacting with the inspection tool, preventing unwanted block activations or planting.
- **[Risk]** Cloud command registration timing on Paper.
  - *Mitigation*: Register commands strictly inside `onEnable()` using `PaperCommandManager.buildOnEnable(plugin)` as required by Paper 1.21.
