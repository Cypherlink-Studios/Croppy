# Proposal: Recipe Discovery and Culinary Compendium

## Why

Currently, Croppy has station processing mechanics (`ProcessDefinition` and `ProcessEngine`), but players have no in-game means to discover what can be cooked without consulting external wikis or admin commands. Furthermore, there is no per-player recipe knowledge tracking or separation between discovering a recipe, knowing its requirements, and executing it.

Implementing an extensible Recipe Discovery Engine and a Culinary Compendium provides players with an intuitive progression system (learning recipes through farming, world exploration, and kitchen experimentation) while keeping discovery mechanics completely decoupled from execution engines.

## What Changes

- **Pure API Contracts (`croppy-api`)**:
  - `KnowledgeState` enum (`UNKNOWN`, `HINTED`, `DISCOVERED`).
  - `PlayerRecipeKnowledge` record tracking player UUID, recipe key, state, timestamp, and discovery source metadata.
  - `RecipeDefinition` record modeling culinary recipes, category, hint configurations, and station bindings.
  - `RecipeDiscoveryMethod` SPI allowing pluggable discovery providers (automatic, experimental, world, etc.).
  - `RecipeKnowledgeStorage` port interface for asynchronous player knowledge persistence.
- **Domain Engine (`croppy-core`)**:
  - `DefaultRecipeDiscoveryEngine`: Manages in-memory player knowledge, registry of discovery methods, and idempotent discovery grants.
  - `RecipeMatcher`: Intelligent ingredient matching with strict balance checks (anti-shadowing precedence) preventing simpler recipes from masking complex combinations.
  - `YamlRecipeLoader`: YAML parser loading recipes, hint presentation rules, and discovery triggers.
- **Platform Adapter (`croppy-paper`)**:
  - `PdcRecipeKnowledgeStorage`: Zero-latency NBT persistence via Paper's `PersistentDataContainer` on player entities.
  - `StationExperimentationHandler`: Integrates experimental cooking into stations, allowing players to discover unknown recipes safely without destroying ingredients.
  - `CompendiumMenuController`: Interactive paginated cookbook GUI (`recipes_menu.yml`) integrated into Croppy's `menu-system`.
  - Cloud Paper 2.0 command integration: `/croppy recipes` (player compendium) and `/croppy recipe give|revoke` (admin).

### Out of Scope
- Culinary mastery / leveling statistics (e.g. tracking total times cooked or quality multipliers), which will be designed as a separate extension layer.
- External database sync or web interfaces (handled via future SQL storage adapters).

## Capabilities

### New Capabilities
- `recipe-discovery`: Governs recipe knowledge states, extensible discovery methods SPI (automatic, experimental, knowledge-based, world-based), exact recipe matching, and player persistence.
- `culinary-compendium`: Governs the interactive recipe catalog GUI (`recipes_menu.yml`), pagination, category filtering, search, and dynamic visual state rendering.

### Modified Capabilities
- `crafting-stations`: Adds support for experimental discovery interactions in crafting stations, decoupling discovery notifications from immediate ingredient consumption.
- `crop-commands`: Adds player compendium navigation command `/croppy recipes` and administrative recipe management commands `/croppy recipe give|revoke|list`.

## Impact

- **New APIs**: New packages `dev.darkblade.croppy.api.recipe.*` and `dev.darkblade.croppy.api.discovery.*`.
- **Existing Systems**: Extends station GUI actions with discovery triggers; connects to existing `menu-system` for rendering the compendium without modifying existing menu engine core code.
- **Zero Breaking Changes**: Existing `ProcessDefinition` and `ProcessEngine` remain fully compatible.
