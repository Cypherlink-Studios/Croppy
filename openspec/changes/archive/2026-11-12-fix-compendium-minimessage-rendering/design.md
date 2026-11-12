# Design: Fix Compendium and Recipe Hint MiniMessage Rendering

## Context

See `proposal.md` for motivation and scope.

In `PaperMessageService.arg(String key, Object value)`, arguments passed as `String` are bound via `Placeholder.unparsed(...)` to prevent untrusted tag injection, whereas arguments passed as `Component` are bound via `Placeholder.component(...)`. Trusted domain definitions such as `RecipeCategory.displayName()` (e.g., `"<aqua>Soups & Stews</aqua>"`) and `HintDefinition.description()` (e.g., `"<gray>A warm simmered...</gray>"`) store MiniMessage-formatted strings. Currently:
- `CompendiumMenuController.renderDiscoveredCard` passes `RecipeCategory.displayName()` as a `String`, causing literal `<aqua>...</aqua>` tag leakage.
- `CompendiumMenuController.renderHintedCard` and `renderUnknownCard` pass `recipe.categoryKey().value()` directly without consulting `RecipeCategoryRegistry`.
- `CompendiumMenuController.renderHintedCard` and `DiscoveryFeedbackHelper.playHintFeedback` pass `HintDefinition.description()` / `hint` as a raw `String` to `compendium.gui.clue-hint`, causing literal tag leakage when hints contain MiniMessage styling.
- `RecipeDetailMenuController.renderResultCard` hardcodes `<gray>Category: <yellow>` + `recipe.categoryKey().value()` and `<gray>Yields: ...` without `RecipeCategoryRegistry` or `PaperMessageService`.
- `CompendiumMenuControllerTest` and `RecipeDetailMenuControllerTest` currently override `renderRecipeCard` / `renderResultCard` with blank stubs because `new ItemStack(Material)` throws when `Bukkit.server` is uninitialized in offline JUnit tests.

## Goals / Non-Goals

**Goals:**
- Resolve registered `RecipeCategory` display names and recipe hint descriptions into parsed Adventure `Component` instances before passing them into `PaperMessageService.render(...)`.
- Preserve full backward compatibility for existing `CompendiumMenuController`, `RecipeDetailMenuController`, and `DiscoveryFeedbackHelper` constructors and null-`messageService` fallbacks.
- Keep `croppy-api` and `croppy-core` untouched (preserving hexagonal module isolation).
- Enable direct, deterministic offline JUnit assertions on the rendered `List<Component>` lore across all compendium states, detail result cards, hint feedback, and `PaperMessageService`.

**Non-Goals:**
- Changing `PaperMessageService.arg(String, Object)` to auto-parse arbitrary `String` arguments (doing so would break untrusted input sanitization guarantees in `localization-system`).
- Modifying existing bundled recipe categories (`croppy:fried`, `croppy:stews`) or adding new categories to `DefaultRecipeCategoryRegistry`.
- Refactoring unrelated callers outside the compendium/recipe hint surface (such as `FertilizerListener` action bars).

## Decisions

### 1. Centralize category resolution in `PaperMessageService`
- **Choice**: Add `resolveCategoryDisplayName(Audience audience, Key categoryKey, RecipeCategoryRegistry categoryRegistry)` (and a convenience `resolveCategoryDisplayName(Audience audience, RecipeCategory category)`) to `PaperMessageService`, matching `resolveRecipeDisplayName` / `resolveItemDisplayName` / `resolveSpeciesDisplayName`.
- **Rationale**: Keeps locale override lookup (`content.category.<namespace>.<value>.name`), MiniMessage deserialization, and unregistered-key fallback (`Component.text(categoryKey.value())`) in one place for both `CompendiumMenuController` and `RecipeDetailMenuController`.
- **Alternative considered**: Deserializing `catName` locally inside `CompendiumMenuController` and `RecipeDetailMenuController` (like `stationComp` is handled locally today). Rejected because duplicating registry lookup, null handling, and fallback across two controllers violates DRY and misses future `content.category.*` locale overrides.

### 2. Deserialize trusted hint text at call sites via `MiniMessage`
- **Choice**: In `CompendiumMenuController.renderHintedCard` and `DiscoveryFeedbackHelper.playHintFeedback`, deserialize the non-null hint description string with `miniMessage.deserialize(...)` into a `Component` before passing it as the `"hint"` argument to `compendium.gui.clue-hint` (and use `Component.append(...)` in the `messageService == null` fallback so unclosed tags cannot bleed).
- **Rationale**: `HintDefinition.description()` is authored in trusted YAML recipe files alongside `displayName`, and `DiscoveryFeedbackHelper` already accepts a `String hint` directly from `RecipeDiscoveryEngine`. Passing a `Component` works seamlessly with `Placeholder.component(...)` without changing `DiscoveryFeedbackProvider` signatures.
- **Alternative considered**: Using `PaperMessageService.parsed("hint", hint)` directly. Rejected because `PaperMessageService.render(Audience, String, Object...)` also invokes `rawArgs(args)` for `{key}` attribute substitution when given `String`s, whereas `Component` args cleanly bypass `rawArgs` and work identically when `messageService == null`.

### 3. Inject `RecipeCategoryRegistry` into `RecipeDetailMenuController` via overloaded constructor
- **Choice**: Add a constructor overload (or nullable field + setter + full constructor) accepting `RecipeCategoryRegistry`, keep the existing 8-argument constructor delegating with `null`, and pass `core.getRecipeCategoryRegistry()` from `CroppyPlugin`. Update `RecipeDetailMenuController.renderResultCard` to use `compendium.gui.yields` and `compendium.gui.category` when `messageService != null`, with matching fallback components otherwise.
- **Rationale**: Zero breaking changes to existing callers or tests while unifying `RecipeDetailMenuController` category and yield rendering with `CompendiumMenuController`.
- **Alternative considered**: Changing the existing constructor in place. Rejected because it would unnecessarily break existing constructor call sites such as `RecipeDetailMenuControllerTest`.

### 4. Extract package-private `Component` lore builders for offline JUnit verification
- **Choice**: Extract `buildDiscoveredLore(Player, RecipeDefinition)`, `buildHintedLore(Player, RecipeDefinition)`, and `buildUnknownLore(Player, RecipeDefinition)` in `CompendiumMenuController`, and `buildResultLore(Player, RecipeDefinition)` in `RecipeDetailMenuController` (reused directly by `render*Card` when applying `meta.lore(...)`). Also extract or keep `DiscoveryFeedbackHelper.buildHintFeedbackMessage(Player, String)` testable with a mocked `Player` proxy.
- **Rationale**: `new ItemStack(Material)` fails in JUnit when `Bukkit.getServer()` is `null`. Returning `List<Component>` from package-private builder methods tests the exact production lore logic directly without reflection hacks or adding MockBukkit dependencies.
- **Alternative considered**: Bootstrapping a full `Server` mock on `Bukkit.setServer(...)`. Rejected as brittle across Paper 1.21.8 registry bootstrap static initializers.

### 5. Null-safe MiniMessage fallback formatting when `messageService == null`
- **Choice**: Share a helper `resolveCategoryComponent(Player player, Key categoryKey)` in both `CompendiumMenuController` and `RecipeDetailMenuController` that delegates to `messageService.resolveCategoryDisplayName(...)` when present, or looks up `categoryRegistry` and calls `miniMessage.deserialize(...)` (falling back to `Component.text(categoryKey.value())`) when `messageService == null`. In the `messageService == null` fallback lines, append the deserialized `Component` rather than concatenating raw strings.
- **Rationale**: Guarantees identical behavior whether `messageService` is wired or omitted in tests, and prevents unclosed tags inside category or hint strings from altering outer component styling.

## Risks / Trade-offs

- [Unregistered custom category keys (e.g. `croppy:fried`, `croppy:stews`) remain plain key values] -> Mitigation: Fallback produces `Component.text(categoryKey.value())` styled by the outer `<yellow>` tag in `compendium.gui.category`, matching current safe fallback behavior without throwing.
- [Nested color tags inside `RecipeCategory.displayName()` override `<yellow>` in `compendium.gui.category`] -> Mitigation: This is intended by Adventure's `Placeholder.component(...)` semantics—categories with explicit color tags (such as `<aqua>Soups & Stews</aqua>`) render in their configured color, while untagged fallback values inherit `<yellow>`.
- [Overview book `{category}` placeholder in `CompendiumMenuController.openCompendiumWithSession`] -> Keep `EvaluationContext` `{category}` aligned with registered `RecipeCategory.displayName()` (falling back to `session.categoryKey().value()`) since `PaperMenuRenderer.replacePlaceholders` substitutes it before `miniMessage.deserialize(...)`.
