# Spec Delta: crop-commands

## ADDED Requirements

### Requirement: Interactive Command Listing and Output Formatting
The system SHALL format all command list outputs (`/croppy item list`, `/croppy recipe list`, `/croppy addon list`) using unified list headers `<gold><bold>=== <Title> (<count>) ===</bold></gold>` and bulleted items ` <dark_gray>•</dark_gray> <yellow><key></yellow> - <info>`, embedding interactive MiniMessage hover tooltips and click actions on all registered identifiers.

#### Scenario: Interactive item list entry
- **WHEN** an administrator runs `/croppy item list`
- **THEN** each displayed item key includes a hover tooltip with item details and a click action that suggests the corresponding `/croppy item give` command.

#### Scenario: Interactive recipe list entry
- **WHEN** an administrator runs `/croppy recipe list`
- **THEN** each displayed recipe key includes a hover tooltip with station/category info and a click action suggesting the grant command.

#### Scenario: Interactive addon list entry
- **WHEN** an administrator runs `/croppy addon list`
- **THEN** each displayed addon ID includes a hover tooltip and a click action that executes `/croppy addon status <id>`.

### Requirement: Localized Command Execution Feedback
The system SHALL deliver all command execution feedback, success messages, administrative notifications, and errors (including addon lifecycle actions and recipe transfers) exclusively through `PaperMessageService` using catalog translation keys prepended with `general.prefix`.

#### Scenario: Localized addon lifecycle feedback
- **WHEN** an administrator executes `/croppy addon load`, `/croppy addon enable`, `/croppy addon disable`, or `/croppy addon unload`
- **THEN** the outcome is reported using localized translation keys with dynamic arguments and `general.prefix`.

#### Scenario: Localized recipe unlock notification
- **WHEN** an administrator unlocks a recipe for a target player via `/croppy recipe give`
- **THEN** the target player receives a localized notification prepended with `general.prefix` in their client locale.
