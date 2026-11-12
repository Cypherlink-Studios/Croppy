# Proposal: Fix Compendium and Recipe Hint MiniMessage Rendering

## Why

In the Culinary Compendium GUI, recipe card lore renders MiniMessage tags literally (for example, `Category: <aqua>Soups & Stews</aqua>`) instead of parsing them into styled Adventure components. This regression was introduced when `CompendiumMenuController` and `DiscoveryFeedbackHelper` migrated to `PaperMessageService.render(...)` while still passing MiniMessage-formatted strings (`RecipeCategory.displayName()` and `HintDefinition.description()`) as raw `String` arguments, which `PaperMessageService.arg(...)` binds via `Placeholder.unparsed(...)`. Meanwhile, `HINTED`/`UNKNOWN` cards and `RecipeDetailMenuController` bypass `RecipeCategoryRegistry` entirely and show raw category key values.

## What Changes

- **Parse Recipe Category Display Names into Components**:
  - Add `resolveCategoryDisplayName(Audience, Key, RecipeCategoryRegistry)` (and/or `RecipeCategory` overload) to `PaperMessageService`, checking optional `content.category.<namespace>.<value>.name` locale overrides and otherwise deserializing the registered `RecipeCategory.displayName()` with MiniMessage (falling back to plain `categoryKey.value()` when unregistered).
  - Update `CompendiumMenuController` (`renderDiscoveredCard`, `renderHintedCard`, and `renderUnknownCard`) to resolve and pass category display names as `Component` arguments to `compendium.gui.category` (with consistent MiniMessage fallback when `messageService` is null).
  - Update `RecipeDetailMenuController` to accept `RecipeCategoryRegistry` (with a backward-compatible constructor overload) and render `renderResultCard` yields and category lines using `messageService` (`compendium.gui.yields`, `compendium.gui.category`) and deserialized category `Component`s instead of hardcoded `recipe.categoryKey().value()`.
- **Parse Recipe Hint Descriptions into Components**:
  - Deserialize `HintDefinition.description()` (and hint feedback text) with MiniMessage into a `Component` before passing `"hint"` to `compendium.gui.clue-hint` in both `CompendiumMenuController.renderHintedCard` and `DiscoveryFeedbackHelper.playHintFeedback`, preventing literal `<gray>...</gray>` tag leakage.
- **Extract Testable Lore Builders and Add Regression Tests**:
  - Expose package-private lore/component builder helpers in `CompendiumMenuController` and `RecipeDetailMenuController` so offline JUnit tests can inspect generated `List<Component>` lore directly without needing a running Bukkit server.
  - Add regression unit tests covering `DISCOVERED`, `HINTED`, and `UNKNOWN` compendium lore, `RecipeDetailMenuController` result lore, `DiscoveryFeedbackHelper` hint chat feedback, and `PaperMessageService.resolveCategoryDisplayName` (asserting zero raw tag leakage and preserved formatting).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `culinary-compendium`: Ensure recipe cards across `DISCOVERED`, `HINTED`, and `UNKNOWN` states, detail inspection result cards, and hint feedback resolve registered category display names and hint descriptions as parsed MiniMessage components without literal tag leakage.
- `localization-system`: Extend content display name resolution in `PaperMessageService` to cover recipe categories (`content.category.<namespace>.<value>.name` override with MiniMessage deserialization fallback).

## Impact

- **Code (`croppy-paper`)**:
  - `PaperMessageService.java`
  - `CompendiumMenuController.java`
  - `RecipeDetailMenuController.java`
  - `DiscoveryFeedbackHelper.java`
  - `CroppyPlugin.java` (wiring `RecipeCategoryRegistry` into `RecipeDetailMenuController`)
- **Tests (`croppy-paper`)**:
  - `CompendiumMenuControllerTest.java`
  - `RecipeDetailMenuControllerTest.java`
  - `PaperMessageServiceTest.java`
  - New or expanded test coverage for `DiscoveryFeedbackHelper`
- **Public API / Modules**: No breaking changes to `croppy-api` or `croppy-core`; existing `RecipeDetailMenuController` constructor signature remains preserved via overload.
