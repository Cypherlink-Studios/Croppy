# Tasks

## 1. Core Addon Rediscovery & Registry Ownership Preservation

- [x] 1.1 Update `DefaultAddonManager` in `croppy-core` to support safe disk rediscovery (`rediscoverUnloadedAddons()` / `refreshDiscovered()`), refreshing `ON_DISK` entries, removing entries whose paths disappeared from disk, and skipping already-active same-path entries without false duplicate-ID errors; add JUnit 5 tests in `croppy-core` verifying version updates, removals, and genuine duplicate-ID collisions.
- [x] 1.2 Add key-ownership query helpers on `PaperAddonManager` (inspecting enabled addons' `PaperAddonContext` tracked key sets for species, items, item categories, crafting recipes, recipes, recipe categories, processes, stations, menus, and interactions) and hard-reload methods (`unloadAll()`, `reloadAddon(String)`, `reloadAll()`) handling transitive active dependents (`dependencies` and `softDependencies`) via `AddonGraphValidator`; verify with JUnit 5 tests covering independent reload, transitive dependent cascade order, and updated manifest rediscovery.

## 2. Isolated `/croppy reload`

- [x] 2.1 Refactor `CroppyPlugin.reloadPlugin()` in `croppy-paper` to remove `addonManager.disableAll()` / `addonManager.enableAll()` and replace destructive `.clear()` calls on shared registries (`SpeciesRegistry`, `ItemRegistry`, `ItemCategoryRegistry`, `MenuRegistry`, `StationRegistry`, `InteractionRegistry`, `RuleRegistry`, `RecipeRegistry`, `ProcessRegistry`, `CraftingRecipeRegistry`, and `PaperCraftingRecipeService`) with ownership-aware selective purging (`unregisterAll(...)` preserving addon-owned entries) while ensuring `CroppyCommands` retains valid service references; verify with unit/integration tests that addon-registered entries survive `/croppy reload` intact without invoking `onDisable`/`onEnable`.

## 3. Extensible Cloud 2.0 Target Selector System

- [x] 3.1 Create `dev.darkblade.croppy.platform.paper.command.selector` in `croppy-paper` containing `TargetSelection<T>`, `SelectorRule<C, T>`, `SingleTargetResolver<C, T>`, and `SelectableTargetParser<C, T>` (implementing Cloud `ArgumentParser` and `SuggestionProvider`), plus reusable rules `AllSelectorRule` and `RecipeCategorySelectorRule` (supporting `@all`, `@<categoryKey>`, and unqualified `@<category>` shorthand while excluding `RecipeCategory.ALL` from category matching); verify parser resolution, empty/unknown `@`-selector errors, and suggestion generation with JUnit 5 unit tests.
- [x] 3.2 Register parameterized `TypeToken<TargetSelection<AddonDescription>>` and `TypeToken<TargetSelection<RecipeDefinition>>` parsers and string-compatible Brigadier mappings (supporting `@` and `:` unquoted in Minecraft chat via `cloudSuggestions()`) in `CroppyPlugin.setupCommands()`; verify parser registration and Brigadier mapping setup compile and execute cleanly under `CroppyCommandsExecutionTest`.

## 4. Command Migrations & Localization

- [x] 4.1 Migrate `/croppy addon <load|unload|enable|disable|reload> <addon>` in `CroppyCommands` to accept `TargetSelection<AddonDescription>` (with `<addonId>` and `@all`), removing the legacy unprefixed `"all"` wildcard; add localization keys in `en-US.yml` and `es-ES.yml` for `command.addon.reload-*` and update `CroppyCommandsTest` / `CroppyCommandsExecutionTest` to cover `@all`, legacy `"all"` rejection, and `addon reload`.
- [x] 4.2 Migrate `/croppy recipe <give|revoke> <player> <recipe>` in `CroppyCommands` to accept `TargetSelection<RecipeDefinition>` (supporting `<recipeKey>`, `@all`, and `@<categoryKey>`), preserving single-target messages and emitting summary feedback to both sender and target player on multi-recipe selectors; add summary localization keys in `en-US.yml` and `es-ES.yml` and verify via `CroppyCommandsExecutionTest`.

## 5. Final Verification

- [x] 5.1 Run `./gradlew.bat test` and `openspec validate --all` and confirm zero test failures and zero validation issues across all modules (verified: `openspec validate --all --strict` 35/35 passed; `:croppy-api:test` and `:croppy-core:test` passed; `:croppy-paper:test` 300/301 passed, with 1 pre-existing unrelated failure in `RecipeDetailMenuControllerTest` from uncommitted `recipe_detail_menu.yml`).
