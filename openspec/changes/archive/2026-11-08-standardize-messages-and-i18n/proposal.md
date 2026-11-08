# Proposal: Standardize Plugin Messages and i18n Consistency

## Why
Croppy currently exhibits severe formatting and localization inconsistencies across its user feedback surfaces:
1. Messages throughout the plugin mix hardcoded English and Spanish strings in Java files (such as `WateringCanService`, `CompendiumMenuController`, `PaperMenuListener`, and `CroppyCommands`), bypassing the `localization-system`.
2. Prefixes are fragmented: chat prompts in `en-US.yml` hardcode `<gold>[Croppy]</gold>` inside translation values while other commands use the gradient prefix `general.prefix`, and subcommands (/croppy addon, recipes) send raw strings without prefixes.
3. Interactive MiniMessage capabilities (`<click>`, `<hover>`) are practically absent across command outputs and notifications, leaving players with static text instead of actionable UX.
4. Cloud 2.0 command errors (`NoPermissionException`, `InvalidSyntaxException`) use unlocalized, unformatted internal fallbacks instead of routing through `PaperMessageService`.

Now that the core simulation, station holograms, and addon systems are stabilized, standardizing all player-facing messaging establishes a polished, coherent, and fully localizable user experience.

## What Changes
- **Enforce Channel-Specific Prefix Rules**:
  - Direct player/sender chat messages MUST prepend `general.prefix`.
  - Action bar notifications and screen Titles/Subtitles MUST omit prefixes to preserve HUD readability and spacing.
  - Remove hardcoded `<gold>[Croppy]</gold>` prefixes from translation keys (`prompt.*`) and Java fallback methods, delegating prefix injection to `PaperMessageService.sendPrefixedMessage()`.
- **Establish Visual & Punctuation Style Conventions**:
  - **Error feedback**: Render in `<red>`, use a sober tone, and terminate with a period (`.`).
  - **Success feedback**: Render in `<green>`, highlight variables in `<yellow>` or `<white>`, and terminate with an exclamation mark (`!`) (including inverted punctuation `¡` in `es-ES.yml`).
  - **List Headers**: Standardize all list headers to `<gold><bold>=== <Title> (<count>) ===</bold></gold>`.
  - **List Items**: Standardize bullet format to ` <dark_gray>•</dark_gray> <yellow><key></yellow> - <info>`.
- **Integrate Interactive MiniMessage Components**:
  - Add `<click:run_command:...>` and `<hover:show_text:'...'>` tags to command suggestions (e.g. `/croppy pack build`, `/croppy recipes`, `/croppy addon status <id>`).
  - Add `<click:suggest_command:...>` to interactive prompts (such as `'cancel'`) and item/recipe list entries.
- **100% i18n Catalog Registration**:
  - Externalize all hardcoded messages from `CroppyCommands.java` (addon management, recipe lists, recipe unlock notifications).
  - Externalize station cooking and cookware temperature errors in `PaperMenuListener.java`.
  - Externalize compendium recipe book GUI labels, category headers, and clues in `CompendiumMenuController.java`.
  - Externalize watering can lore from `WateringCanService.java` to `en-US.yml` and `es-ES.yml` (removing hardcoded Spanish from Java).
  - Externalize crop clearance action bar warnings in `CropPlantListener.java`.
  - Localize recipe discovery fanfare titles and subtitles in `DiscoveryFeedbackHelper.java`.
- **Cloud 2.0 Command Exception Localization**:
  - Register an `ExceptionController` on `PaperCommandManager` to intercept `NoPermissionException` (using `general.no-permission`) and invalid syntax/argument exceptions with localized, prefixed MiniMessage components.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `localization-system`: Expanding message catalog contracts and rendering specifications to cover channel prefix rules, standardized styling and punctuation conventions, interactive MiniMessage tag support, and command exception handling.
- `crop-commands`: Updating command feedback requirements to mandate localized message delivery with interactive click/hover elements and unified list formatting.

## Impact
- **Code**: `croppy-paper` module: `CroppyCommands.java`, `CroppyPlugin.java`, `PaperMessageService.java`, `CompendiumMenuController.java`, `PaperMenuListener.java`, `CropPlantListener.java`, `WateringCanService.java`, `DiscoveryFeedbackHelper.java`.
- **Locales**: `croppy-paper/src/main/resources/locales/en-US.yml` and `es-ES.yml`.
- **Tests**: `LocaleResourcesIntegrityTest.java` and Paper command tests to verify catalog synchronization and formatting integrity.
