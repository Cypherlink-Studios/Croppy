# Tasks

## 1. Locale Catalogs Standardization

- [x] 1.1 Clean and expand `locales/en-US.yml`: strip embedded `[Croppy]` prefixes from `prompt.*`, standardize error periods (`.`) and success exclamations (`!`), integrate MiniMessage `<click>` and `<hover>` tags on actionable command references, and add missing translation sections (`command.addon.*`, `command.recipe.*`, `command.invalid-syntax`, `crop.plant.*`, `station.cooking.*`, `compendium.gui.*`, `watering.lore.*`, and `recipe.fanfare.*`).
- [x] 1.2 Synchronize `locales/es-ES.yml` with all newly added and updated keys, applying proper Spanish inverted punctuation (`¡`, `¿`), accurate accentuation, and localized click/hover tooltips. Verify via `LocaleResourcesIntegrityTest`.

## 2. Message Service & Prompt Routing

- [x] 2.1 Update `PaperMessageService` to support GUI component rendering helpers and ensure untrusted replacement values cannot inject unauthorized tags.
- [x] 2.2 Update `ChatPromptService` to dispatch chat prompts via `sendPrefixedMessage()` and replace legacy `<gold>[Croppy]</gold>` fallbacks with the standard `general.prefix` gradient.

## 3. Subsystem & Listener String Externalization

- [x] 3.1 Refactor `WateringCanService` to load item lore lines from `PaperMessageService` (`watering.lore.*`), completely removing hardcoded Spanish text from the Java class.
- [x] 3.2 Update `CropPlantListener` to dispatch localized action bar messages for crop clearance limits (`crop.plant.not-enough-space` and `crop.plant.not-enough-vertical-space`).
- [x] 3.3 Update `PaperMenuListener` to dispatch localized prefixed messages for cooking errors (missing ingredients, insufficient temperature, no recipe match).
- [x] 3.4 Update `CompendiumMenuController` to localize menu layout errors and render all recipe card lore, clues, and category labels using `PaperMessageService`.
- [x] 3.5 Update `DiscoveryFeedbackHelper` to render localized recipe discovery titles, subtitles, and prefixed chat notifications.

## 4. Commands & Cloud 2.0 Exception Controller

- [x] 4.1 Refactor `CroppyCommands.java` to route all addon management commands, recipe lists, and recipe unlocks through `sendPrefixed()`, formatting list headers as `=== <Title> (<count>) ===` and adding interactive `<click>` and `<hover>` actions to item, recipe, and addon IDs.
- [x] 4.2 Register an `ExceptionController` on `PaperCommandManager` in `CroppyPlugin.java` to intercept `NoPermissionException` and invalid command syntax, dispatching localized prefixed messages.

## 5. Verification Gate

- [x] 5.1 Execute `./gradlew test` to verify that all unit test suites, command parsers, and `LocaleResourcesIntegrityTest` pass with zero failures.

## Workflow follow-up

- Archive the change after implementation and test verification.
- Verify durable specification synchronization for `localization-system` and `crop-commands`.
