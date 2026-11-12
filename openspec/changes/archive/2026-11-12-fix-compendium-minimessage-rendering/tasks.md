# Tasks

## 1. Category Display Name Resolution in PaperMessageService

- [x] 1.1 Add `resolveCategoryDisplayName(Audience, RecipeCategory)` and `resolveCategoryDisplayName(Audience, Key, RecipeCategoryRegistry)` to `PaperMessageService` supporting optional `content.category.<namespace>.<value>.name` locale overrides, MiniMessage deserialization of registered `RecipeCategory.displayName()`, and `Component.text(categoryKey.value())` fallback for unregistered keys; verify compilation with `.\gradlew.bat :croppy-paper:compileJava`.
- [x] 1.2 Add unit tests in `PaperMessageServiceTest` covering registered category MiniMessage rendering (`<aqua>Soups & Stews</aqua>` without literal `<aqua>` leakage), `content.category.<namespace>.<value>.name` locale override resolution, and unregistered category key fallback; verify with `.\gradlew.bat :croppy-paper:test --tests "*PaperMessageServiceTest*"`.

## 2. Compendium Lore and Hint Feedback MiniMessage Rendering

- [x] 2.1 Refactor `CompendiumMenuController` to resolve recipe categories as `Component`s via `resolveCategoryComponent(Player, Key)` across `renderDiscoveredCard`, `renderHintedCard`, and `renderUnknownCard`, align `openCompendiumWithSession` `{category}` context resolution, deserialize `recipe.hint().description()` before passing `"hint"` to `compendium.gui.clue-hint`, and extract package-private `buildDiscoveredLore`, `buildHintedLore`, and `buildUnknownLore` helpers; verify compilation with `.\gradlew.bat :croppy-paper:compileJava`.
- [x] 2.2 Update `DiscoveryFeedbackHelper.playHintFeedback` to deserialize MiniMessage-formatted hint text into a `Component` before passing it to `messageService.render(player, "compendium.gui.clue-hint", "hint", ...)` and safely append components in the null-`messageService` fallback; verify compilation with `.\gradlew.bat :croppy-paper:compileJava`.
- [x] 2.3 Add regression tests in `CompendiumMenuControllerTest` and a `DiscoveryFeedbackHelperTest` verifying `DISCOVERED`, `HINTED`, and `UNKNOWN` lore lines and hint chat feedback with `PlainTextComponentSerializer` (no literal `<aqua>`, `<gray>`, or closing tags) and `MiniMessage.miniMessage().serialize(...)` (preserved styling), plus null-`messageService` and unregistered-category fallbacks; verify with `.\gradlew.bat :croppy-paper:test --tests "*CompendiumMenuControllerTest*" --tests "*DiscoveryFeedbackHelperTest*"`.

## 3. Recipe Detail Result Card Category & Yield Localization

- [x] 3.1 Add a `RecipeCategoryRegistry` parameter to `RecipeDetailMenuController` (keeping the existing 8-argument constructor as a delegating overload), wire `core.getRecipeCategoryRegistry()` in `CroppyPlugin`, extract package-private `buildResultLore(Player, RecipeDefinition)`, and render result yields (`compendium.gui.yields`) and parsed category (`compendium.gui.category`) via `PaperMessageService` with component-safe fallback; verify compilation with `.\gradlew.bat :croppy-paper:compileJava`.
- [x] 3.2 Add unit tests in `RecipeDetailMenuControllerTest` verifying `buildResultLore` renders localized yields and parsed MiniMessage category display names without raw tag leakage and handles unregistered categories and null `messageService`; verify with `.\gradlew.bat :croppy-paper:test --tests "*RecipeDetailMenuControllerTest*"`.

## 4. Full Suite & OpenSpec Validation

- [x] 4.1 Run the full project test suite (`.\gradlew.bat test`) and confirm all unit and integrity tests across `croppy-api`, `croppy-core`, and `croppy-paper` pass with zero failures.
- [x] 4.2 Run `openspec validate fix-compendium-minimessage-rendering --strict` and verify all OpenSpec change artifacts and delta specs pass strict validation cleanly.
