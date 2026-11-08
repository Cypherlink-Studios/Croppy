# Design: Standardize Plugin Messages and i18n Consistency

## Context
See `proposal.md` for problem background and motivation.
Currently, `PaperMessageService` sits between pure domain logic and Paper/Kyori Adventure, backed by `DefaultMessageCatalog` which loads `locales/*.yml`. While `PaperMessageService` provides `sendPrefixedMessage()`, `sendMessage()`, and `sendActionBar()`, the codebase bypasses this abstraction in multiple subsystems, creating fragmented user feedback with hardcoded colors, mismatched prefixes (`<gold>[Croppy]</gold>`), and unlocalized strings.

## Goals / Non-Goals

**Goals:**
- Unify all player feedback under `PaperMessageService`.
- Enforce strict channel rules: Chat is prefixed; Action bar and Titles are unprefixed.
- Remove all hardcoded player-facing strings from Java files (`croppy-paper`).
- Standardize colors, punctuation, list headers, and bullets across `en-US.yml` and `es-ES.yml`.
- Add interactive `<click>` and `<hover>` tags to actionable messages and commands.
- Register an `ExceptionController` in Cloud 2.0 to catch unauthorized execution and syntax errors.
- Ensure 100% test pass on `LocaleResourcesIntegrityTest`.

**Non-Goals:**
- Modifying pure domain logic or contracts in `croppy-core` or `croppy-api` (this is purely an adapter/presentation layer refinement).
- Redesigning the underlying Crowdin workflow or locale file storage formats.
- Adding runtime dynamic player-switched locales independent of client Minecraft locale settings.

## Decisions

### 1. Delivery Channel Rules and Prefix Routing
- **Decision**: Chat messages delivered via `PaperMessageService.sendPrefixedMessage()` resolve the player's client locale, render the message, and prepend `general.prefix`. Action bar notifications (`sendActionBar()`) and titles/subtitles directly render the localized template without `general.prefix`.
- **Rationale**: Action bars have limited screen space on default GUI scales; prepending prefixes truncates critical feedback. Chat messages, conversely, require clear server/plugin branding.
- **Alternatives Considered**: Automatically prepending prefix inside `PaperMessageService.render()`. Rejected because it would leak prefixes into Action Bars, GUI Item Lore, and Titles.

### 2. Purification of Resource Templates (`locales/*.yml`)
- **Decision**: Strip all occurrences of `<gold>[Croppy]</gold>` and other embedded prefixes from translation keys (`prompt.*`). All chat prompts will be sent via `sendPrefixedMessage()`.
- **Rationale**: If a server administrator configures a custom `general.prefix` (e.g. `<green>[Farming]</green> `), all chat messages (including prompts) will seamlessly and automatically reflect the custom prefix without leftover hardcoded tokens.

### 3. Visual & Interactive Formatting Conventions
- **Decision**:
  - Errors: `<red>` body, neutral tone, ending with a period (`.`).
  - Success: `<green>` body, parameters in `<yellow>` or `<white>`, ending with an exclamation mark (`!`) (with `¡` in `es-ES.yml`).
  - List Headers: `<gold><bold>=== <Title> (<count>) ===</bold></gold>`.
  - Bullets: ` <dark_gray>•</dark_gray> <yellow><key></yellow> - <info>`.
  - Interactive Tags:
    - In lists, item/recipe keys wrap in `<hover:show_text:'...'>` and `<click:suggest_command:...>`.
    - In notices, command references wrap in `<hover:show_text:'...'>` and `<click:run_command:...>`.
    - In chat prompts, `'cancel'` wraps in `<hover:show_text:'...'>` and `<click:suggest_command:cancel>`.

### 4. Cloud 2.0 Exception Interception
- **Decision**: In `CroppyPlugin.registerCommands()`, attach an `ExceptionController` to `PaperCommandManager`:
  - `NoPermissionException`: Send `general.no-permission` via `sendPrefixedMessage()`.
  - `InvalidSyntaxException` / `ArgumentParseException`: Send localized `command.invalid-syntax` via `sendPrefixedMessage()`.
- **Rationale**: Eliminates default English Cloud messages, ensuring consistent branding and full Spanish localization when non-op or non-English players interact with commands.

### 5. Structure of New Translation Keys
New keys added to both `locales/en-US.yml` and `locales/es-ES.yml`:
- `command.addon.*`: list headers, empty state, entries, info labels, and load/enable/disable/unload success/error strings.
- `command.recipe.*`: list headers, entry format, admin grant notification to recipient (`recipe.unlocked_by_admin`).
- `command.invalid-syntax`: usage syntax error string.
- `crop.plant.*`: `not-enough-space` and `not-enough-vertical-space`.
- `station.cooking.*`: `missing-ingredients`, `not-hot-enough`, `no-recipe-match`.
- `compendium.gui.*`: `missing-config`, `no-slots`, `category`, `station`, `required-ingredients`, `status-discovered`, `clue-title`, `clue-hint`, `clue-components`, `secret-ingredient`, `unknown-title`, `unknown-experiment`.
- `watering.lore.*`: `area`, `water-bar`, `refill-hint`.
- `recipe.fanfare.*`: `title`, `subtitle`.

## Risks / Trade-offs

- **[Risk] MiniMessage Tag Injection on dynamic user inputs**: If user inputs or unverified strings are concatenated into MiniMessage strings, players could inject unauthorized formatting or click commands.
  - $\rightarrow$ **Mitigation**: Never concatenate raw strings directly into `miniMessage.deserialize()`. Always use `PaperMessageService.args()` with Adventure `TagResolver` or sanitize using `Component.text()` before tag replacement.
- **[Risk] Secondary Locale Divergence (`es-ES.yml`)**: Adding keys to `en-US.yml` without updating `es-ES.yml` causes build test failures.
  - $\rightarrow$ **Mitigation**: Add corresponding Spanish translations for every newly introduced key simultaneously and verify with `LocaleResourcesIntegrityTest`.

## Migration Plan
1. Update `locales/en-US.yml` and `locales/es-ES.yml` with cleaned and newly added keys.
2. Update `PaperMessageService.java` with helper methods for GUI and title localization if needed.
3. Update listeners and controllers (`CroppyCommands`, `CompendiumMenuController`, `WateringCanService`, `PaperMenuListener`, `CropPlantListener`, `DiscoveryFeedbackHelper`).
4. Register Cloud 2.0 exception handlers in `CroppyPlugin.java`.
5. Run `./gradlew.bat test` to verify zero regression and clean locale integrity.
