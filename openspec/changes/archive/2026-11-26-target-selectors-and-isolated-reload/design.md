# Design

## Context

See `proposal.md` for motivation and scope. Current codebase constraints discovered during exploration:

1. **Shared registry `.clear()` in `CroppyPlugin.reloadPlugin()`**: `reloadPlugin()` currently calls `.clear()` on every shared registry (`SpeciesRegistry`, `ItemRegistry`, `ItemCategoryRegistry`, `MenuRegistry`, `StationRegistry`, `InteractionRegistry`, `RuleRegistry`, `RecipeRegistry`, `ProcessRegistry`, `CraftingRecipeRegistry`, plus `craftingRecipeService.unregisterAll()` and `fertilizerRegistry.clear()`), and then compensates by calling `addonManager.disableAll()` + `addonManager.enableAll()`. Removing the addon lifecycle calls without selective registry purging would erase all addon-registered content on `/croppy reload`.
2. **Brigadier key mapping constraint**: `CroppyPlugin.setupCommands()` maps `KeyParser` and `SpeciesParser` to Brigadier `ArgumentTypes.key()`. Minecraft's `ResourceLocationArgument` rejects `@` client-side before command dispatch, so selector-capable arguments cannot use `ArgumentTypes.key()`.
3. **Duplicate-ID rejection in `DefaultAddonManager.discoverAddons()`**: `registerDiscovered()` rejects any `addonId` already present in `discoveredAddons` with a `CRITICAL: Duplicate addon id` error, preventing re-scanning of unloaded addons unless entries are cleared/refreshed first.
4. **ClassLoader DAG linking**: `PaperAddonManager.loadAddon()` links both `dependencies` and `softDependencies` `AddonClassLoader`s. Hard-reloading an upstream addon without unloading its active dependents leaves dependents referencing a closed `AddonClassLoader`.

## Goals / Non-Goals

**Goals:**
- Isolate `/croppy reload` so it reloads only Croppy's own config, locales, and core definitions while leaving enabled addons and their registered entries intact.
- Provide `/croppy addon reload <<addonId> | @all>` as a true hard reload (unload + disk rediscovery + DAG-ordered enable) with transitive dependent cascade.
- Build a reusable, extensible Cloud 2.0 `@`-prefixed target selector framework (`TargetSelection<T>` + `SelectableTargetParser<C, T>` + `SelectorRule<C, T>`) usable by addon and recipe commands today and easy to extend for future commands.
- Unify `/croppy addon <load|unload|enable|disable|reload>` on `@all` and support `@all` / `@<categoryKey>` on `/croppy recipe <give|revoke>` with concise summary messages.

**Non-Goals:**
- Hot-reloading Java classes of Croppy itself or migrating `CroppyPlugin.reloadPlugin()` away from its existing synchronous Paper main-thread execution model.
- Supporting `@`-selectors on `/croppy item give`, `/croppy inspect`, `/croppy fertilize`, or `/croppy debug` in this change (though the abstraction will support them in future changes).
- Introducing a new `croppy-api` dependency on Incendo Cloud (Cloud remains confined to `croppy-paper`).

## Decisions

### 1. Ownership-Aware Selective Purge on `/croppy reload`
- **Decision**: Remove `addonManager.disableAll()` and `addonManager.enableAll()` from `CroppyPlugin.reloadPlugin()`, and replace full `.clear()` calls on shared registries with ownership-aware removal via `Registry.unregisterAll(entry -> !isAddonOwned(type, entry.getKey()))` (where `PaperAddonManager` exposes ownership queries over enabled addons' `PaperAddonContext` tracked key sets). For `CraftingRecipeRegistry` + `PaperCraftingRecipeService`, unregister only non-addon crafting recipes from both `CraftingRecipeRegistry` and Bukkit (`Bukkit.removeRecipe`).
- **Alternative considered**: Snapshot and re-insert addon entries after `.clear()`. Rejected because `unregisterAll` avoids transient empty-registry states and requires no cloning/re-registration of objects (and avoids touching `ItemCategory` or `RecipeCategory` instances created by Java addons).
- **Note on `setupItemDefinitions()` / `setupFertilizers()` / `setupLocalization()`**: Ensure `CroppyCommands` and other consumers either query dynamic getters or receive updated service references on reload (or reuse existing registry instances instead of replacing `this.fertilizerRegistry` when possible). Specifically, `fertilizerRegistry.clear()` can stay as-is since `PaperAddonContext.getFertilizerRegistry()` currently returns `null`, while `ItemRegistry`, `SpeciesRegistry`, `ItemCategoryRegistry`, `MenuRegistry`, `StationRegistry`, `InteractionRegistry`, `RecipeRegistry`, `ProcessRegistry`, `CraftingRecipeRegistry` must preserve addon-owned keys.

### 2. Hard Addon Reload & Safe Rediscovery
- **Decision**:
  - Add a `rediscoverUnloadedAddons()` / `refreshDiscovered()` capability on `DefaultAddonManager` (in `croppy-core`):
    - Removes `discoveredAddons`, `addonPaths`, and `dataFolders` entries for addons currently in `AddonState.ON_DISK` (unloaded) before re-scanning `addonsDirectory`, so updated manifests/JARs are re-parsed cleanly and deleted addons disappear without false duplicate-ID errors.
    - Enabled/loaded addons keep their active entries during scan and are skipped silently when their existing `id` matches the same path (only logging duplicate errors when a *second distinct path* declares an already-active `id`).
  - In `PaperAddonManager`:
    - `reloadAddon(String addonId)`:
      1. Validate `addonId` exists in `discoveredAddons` (return failure result if unknown).
      2. Compute the transitive closure of active (`LOADED` or `ENABLED`) dependents (considering both `dependencies` and `softDependencies`) using `AddonGraphValidator.resolveOrder(...)`.
      3. Record which addons in that closure were `ENABLED` vs `LOADED`, plus the target addon itself.
      4. Unload them in **reverse topological order** (`unloadAddon(...)`) so every `AddonClassLoader` and tracked registry entry is cleanly severed.
      5. Call `rediscoverUnloadedAddons()`.
      6. Re-enable the target addon (if it was `ENABLED` or `ON_DISK`/`LOADED` — reloading an explicit target enables it) and re-enable any previously `ENABLED` dependents in **topological order** via `AddonGraphValidator.resolveOrder(...)`.
    - `reloadAll()`:
      1. `disableAll()`, unload all discovered addons, call `rediscoverUnloadedAddons()`, then `enableAll()`.
- **Alternative considered**: Soft reload (`disableAddon` + `enableAddon` keeping `AddonClassLoader`). Rejected per user requirement for true JAR/class hot-swapping on `/croppy addon reload`.

### 3. Extensible Cloud 2.0 Target Selector Architecture (`croppy-paper`)
- **Decision**: Create a dedicated package `dev.darkblade.croppy.platform.paper.command.selector` containing:
  - `TargetSelection<T>` (Java 21 record): `String rawInput`, `boolean selector`, `Optional<String> selectorToken`, `List<T> targets` (immutable copy).
  - `SelectorRule<C, T>` (interface):
    - `boolean matches(String token)` (token without or with `@`, e.g., `@all`, `@<category>`).
    - `ArgumentParseResult<List<T>> resolve(CommandContext<C> context, String token)` (returns failure if matched rule yields empty or invalid target).
    - `List<String> suggestions(CommandContext<C> context, String remaining)`.
  - `SelectableTargetParser<C, T>` (implements `ArgumentParser<C, T>` / `ArgumentParser<C, TargetSelection<T>>` and `SuggestionProvider<C>`):
    - Configured via a clean fluent `Builder<C, T>` accepting ordered `SelectorRule<C, T>` instances and a fallback `SingleTargetResolver<C, T>` + single-target suggestion supplier.
    - When input starts with `@`: matches the first `SelectorRule` whose `matches(input)` is true; if none matches, returns `ArgumentParseResult.failure(...)` (never falls back to single-target parsing for `@...` inputs).
    - When input does not start with `@`: delegates to `SingleTargetResolver` and wraps the result in `TargetSelection.single(rawInput, target)`.
    - `suggestionsFuture`: collects suggestions from all registered `SelectorRule`s + single-target resolver, filtered by current prefix (`remaining`).
  - Built-in reusable rules:
    - `AllSelectorRule`: matches `@all` (case-insensitive) and resolves `Supplier<Collection<T>>` (failing cleanly if empty, e.g., no addons or no recipes).
    - `RecipeCategorySelectorRule`: matches `@<key>` (excluding `@all`, which `AllSelectorRule` handles first, and skipping `RecipeCategory.ALL` in suggestions). Parses `@<category>` via `Key.parse(stripped)` so `@soups` resolves to `croppy:soups` and `@myaddon:drinks` resolves to `myaddon:drinks`. Validates that the category exists in `RecipeCategoryRegistry` (or matches recipes if registry is absent in unit tests) and filters `RecipeRegistry` by `recipe.categoryKey().equals(categoryKey)`.
- **Typed Parameter Bindings in Cloud 2.0**:
  - Cloud 2.0's `ParserRegistry` matches parsers by `TypeToken`. Because `TargetSelection<AddonDescription>` and `TargetSelection<RecipeDefinition>` share the erased raw class `TargetSelection` unless registered with exact `TypeToken`s, register each parameterized `TypeToken`:
    - `new TypeToken<TargetSelection<AddonDescription>>() {}`
    - `new TypeToken<TargetSelection<RecipeDefinition>>() {}`
  - **Brigadier Mapping**: Do NOT map `SelectableTargetParser` to `ArgumentTypes.key()`. Leave it mapped via `StringArgumentType.word()` (or Cloud's default greedy/string mapping with `.cloudSuggestions()`), ensuring `@all` and `@croppy:soups` pass client-side Brigadier validation while still receiving full Cloud dynamic suggestions.
- **Alternative considered**: Keeping `Key` / `String` parameters with `@Suggestions` and `if (arg.startsWith("@"))` inside `CroppyCommands`. Rejected because parsing, validation, and suggestion logic would be duplicated per command and could not fail gracefully at Cloud's parse/suggestion layer.

### 4. Command Semantics & Bulk Recipe Feedback
- **`/croppy addon <load|unload|enable|disable|reload> <addon>`**:
  - All 5 lifecycle subcommands accept `TargetSelection<AddonDescription>` supporting `<addonId>` and `@all`.
  - Unprefixed `"all"` is no longer treated as a wildcard (unless an actual addon has id `"all"`).
  - `load @all` runs `discoverAddons()` / `rediscoverUnloadedAddons()` then loads each `ON_DISK` addon; `enable @all` runs `enableAll()`; `disable @all` runs `disableAll()`; `unload @all` runs `unloadAll()` (in reverse topological order); `reload @all` runs `reloadAll()`.
- **`/croppy recipe <give|revoke> <target> <recipe>`**:
  - Both subcommands accept `TargetSelection<RecipeDefinition>` supporting `<recipeKey>`, `@all`, and `@<categoryKey>`.
  - Edge case for `revoke @all`: in addition to `RecipeRegistry` entries, if `@all` is used with `revoke`, should we iterate `recipeRegistry.values()` or `discoveryEngine.getAllKnowledge(target.getUniqueId()).keySet()`? Iterating the union (all keys in `recipeRegistry` plus any orphaned keys in `getAllKnowledge` when `@all` is used, or `recipeRegistry` filtered for `@category`) ensures `@all` genuinely revokes everything known while `@category` revokes all recipes in that category. Even simpler: `RecipeRegistry` contains all active recipes, and for `@all` revoke we can check `discoveryEngine.getAllKnowledge(uuid)` unioned with `recipeRegistry` or operate over the resolved `TargetSelection<RecipeDefinition>` + clean summary count. Let's keep `TargetSelection<RecipeDefinition>` pure (resolving from `RecipeRegistry`) and document clearly that `give`/`revoke` operate on registered recipes matched by the selector, while single-key `revoke` can still support revoking a `Key`? Wait: if `SingleTargetResolver` resolves `RecipeDefinition` from `RecipeRegistry`, both `give` and `revoke` validate unknown recipe keys consistently!
  - **Feedback behavior**:
    - Single-target selection (`!selection.isSelector()`): preserves existing single-recipe messages (`recipe.admin_give_success`, `recipe.already_known`, `command.recipe.unlocked_by_admin`, `recipe.admin_revoke_success`, `recipe.not_known`).
    - Multi-target selector (`selection.isSelector()`): iterates targets, persists changes to `recipeStorage`, counts `affected` vs `skipped` (already known / not known), and sends **one summary message** to the admin (and one summary message to the target player if `affected > 0` on `give`).

## Risks / Trade-offs

- **[Risk] `reloadPlugin()` currently recreates `ItemFactory`, `PaperMessageService`, and `FertilizerRegistry` instances** -> **Mitigation**: Keep registry instances (`core.get*Registry()`) stable in `CroppyCore` (already the case) and audit `reloadPlugin()` so `CroppyCommands` / `PaperAddonManager` aren't left with stale references; document in `tasks.md` to verify `CroppyCommands` references remain valid after `/croppy reload`.
- **[Risk] Brigadier `ArgumentTypes.key()` vs `StringArgumentType.word()` for colons (`croppy:tomato_soup`)** -> **Mitigation**: In Brigadier, `StringArgumentType.word()` accepts `[A-Za-z0-9_+.-]` — wait: Brigadier's `word()` (`readUnquotedString`) allows `a-z`, `A-Z`, `0-9`, `_`, `-`, `.`, `+` and **DOES NOT allow `:` or `@`!** (`StringReader.isAllowedInUnquotedString` rejects BOTH `:` and `@`!). **CRITICAL CATCH**: In Cloud Paper / Brigadier, `word()` fails on `:` and `@`, whereas `StringArgumentType.string()` (quoted or single token? No, `string()` ALSO uses `readUnquotedString()` unless quoted!) or `StringArgumentType.greedyString()`! Wait: how does Cloud 2.0's default `StringParser.stringParser()` map to Brigadier? Cloud's `PaperCommandManager` maps `StringParser` (`StringMode.SINGLE`) to `StringArgumentType.word()` or `greedyString()` — we MUST verify in `croppy-paper` tests/Brigadier mapping! If `StringParser.SINGLE` maps to `word()`, then `@all` or `croppy:tomato_soup` as an unquoted string in Minecraft chat needs `ArgumentTypes` or `StringArgumentType.greedyString()` (for trailing arguments) OR a custom Cloud Brigadier mapping to `StringArgumentType.greedyString()` when it's the last argument (note: both `/croppy addon <op> <addon>` and `/croppy recipe <op> <player> <recipe>` have the selector as the **final** argument!). Let's explicitly verify Cloud 2.0's `PaperBrigadierMappings` behavior during implementation and use `greedyString()` (reading a single token via `readString()`) or Cloud's native greedy/string mapping so `@` and `:` both pass Brigadier without quotes!
- **[Risk] Hard-reloading an addon whose dependency failed to load after disk edit** -> **Mitigation**: If unloading succeeds and re-enabling fails due to a broken manifest or missing dependency, log clearly at `ERROR` on `LogChannel.ADDON` and report failure to the sender rather than leaving partial state.
