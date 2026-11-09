# Design: Enhanced Culinary Compendium & Recipe Inspection Architecture

## Context

The Culinary Compendium was implemented as a simple 54-slot chest GUI backed by `recipes_menu.yml`, rendering static category tabs and `DECORATION` slots for recipes. Refer to `proposal.md` for background and motivation.

To maintain strict Hexagonal Architecture (`AGENTS.md`):
- `croppy-api` must contain only pure Java 21 ports (`RecipeCategoryRegistry`).
- `croppy-core` handles domain registries and filtering engines without Bukkit dependencies.
- `croppy-paper` handles Bukkit inventory holders, click interception, MiniMessage item styling, and proximity scans.

## Goals / Non-Goals

**Goals:**
- Provide a first-class `RecipeCategoryRegistry` enabling dynamic and addon-contributed meal categories.
- Deliver an interactive recipe detail view (`recipe_detail_menu.yml`) displaying step-by-step sequential cooking phases, thermal ranges, and failure policies.
- Implement robust search (`/croppy recipes search <query>`) and "Ready to Cook" inventory filtering.
- Enrich recipe card tooltips with localized item/station names, duration, thermal bounds, and nearby station detection.
- Add configuration settings controlling unknown recipe visibility and detail view behavior.

**Non-Goals:**
- Modifying the underlying crafting execution engine (`ProcessEngine`) or altering station physics.
- Creating an external web database or standalone book renderer outside the Paper inventory GUI framework.
- Replacing existing recipe file formats in `recipes/*.yml`.

## Decisions

### 1. Dedicated `RecipeCategoryRegistry` Port and Lifecycle
* **Decision**: Introduce `RecipeCategoryRegistry` extending `Registry<RecipeCategory>, Service` in `croppy-api`, implemented by `DefaultRecipeCategoryRegistry` in `croppy-core`.
* **Rationale**: Decouples categories from hardcoded enums/records. Addons (such as `croppy-coffee`) can register categories during activation and clean them up via `AddonDecommissioner`.
* **Alternatives Considered**:
  - *Hardcoding addon categories into core*: Violates modularity; core cannot know about specific addons.
  - *Deriving categories dynamically solely from loaded recipes*: Lacks metadata such as category display icons, custom gradient names, and ordering.

### 2. Recipe Detail Inspection Sub-Menu & Backstack
* **Decision**: Create a dedicated `RecipeDetailMenuController` backed by `menus/recipe_detail_menu.yml`. When a player clicks an unlocked recipe card slot in `CompendiumMenuController`, the click event opens the detail menu.
* **Timeline Presentation**: The controller inspects `ProcessRegistry.get(recipe.processKey())` to map each `PhaseDefinition` into interactive phase indicator items:
  - Phase Type (`TIMED`, `MANUAL_INTERACTION`, `ENVIRONMENTAL`)
  - Operating thermal range extracted from condition expressions (e.g. `120°C - 180°C`)
  - Duration/timeouts in human-readable seconds (`12.5s`)
  - Failure/Spoil consequences (e.g., `Spoils into: Ash`)
* **Session Backstack**: The viewing player's active session retains the originating page, category, and active search filter, allowing seamless return via the `[< Back]` action.
* **Alternatives Considered**:
  - *Sending steps to chat*: Unstructured, gets lost in chat logs, and lacks visual appeal.
  - *Overloading the main recipe card lore*: Tooltip becomes too large to fit on screen for complex 4-5 phase recipes.

### 3. Extended Compendium Session and Stream Filtering
* **Decision**: Expand `CompendiumSession` to store:
  ```java
  public record CompendiumSession(
      int page,
      Key categoryKey,
      String searchQuery,
      boolean onlyReadyToCook,
      int categoryPage
  )
  ```
* **Filtering Pipeline**:
  1. Filter by category (or all if `croppy:all`).
  2. If `searchQuery != null`, filter by case-insensitive token match across recipe display name, result item name, and input ingredients.
  3. If `onlyReadyToCook`, filter for recipes where `countPlayerItem(player, input) >= input.amount()`.
  4. If `show-unknown-recipes` is `false`, filter out `KnowledgeState.UNKNOWN` recipes.
  5. Sort: Ready-to-cook discovered -> Other discovered -> Hinted -> Unknown.
* **Alternatives Considered**:
  - *Precomputing filtered lists per tick*: Unnecessary memory overhead; filtering 50-200 recipes in memory takes <1ms upon GUI open/page change.

### 4. Station Proximity Detection
* **Decision**: Add a proximity helper scanning `DefaultStationManager.getStationsInChunk(...)` for the player's current and adjacent chunks within a configurable radius (default: 5 blocks).
* **Rationale**: Lightweight, thread-safe, and avoids expensive world block iteration.

### 5. Active Tab Visual Feedback
* **Decision**: When rendering category tab items in `CompendiumMenuController`, apply `Enchantment.UNBREAKING` (with `ItemFlag.HIDE_ENCHANTS`) and prefix the title with `<yellow><bold>▶ <title> ◀</bold></yellow>` if the tab matches the active session category.
* **Rationale**: Provides clear, immediate feedback on which category filter is active without altering menu layout definitions.

## Risks / Trade-offs

* **[Risk] High recipe count with heavy search queries** $\rightarrow$ *Mitigation*: Memoize normalized search strings or run filtering on a synchronized copy without blocking server ticks.
* **[Risk] Missing `ProcessDefinition` for legacy/custom recipes** $\rightarrow$ *Mitigation*: Detail view handles absent processes gracefully, falling back to basic ingredient and output summary.
* **[Risk] Addons failing to unregister categories** $\rightarrow$ *Mitigation*: Track category keys in `PaperAddonContext` and automatically purge them in Phase 4 of `AddonDecommissioner`.

## Migration Plan

1. Core and API changes are non-breaking additive extensions.
2. Existing `recipes_menu.yml` files remain compatible; bundled default is updated with category navigation and search elements.
3. Default `recipe_detail_menu.yml` is automatically extracted to `menus/` if absent.
4. `config.yml` extracts new `compendium:` configuration section.
