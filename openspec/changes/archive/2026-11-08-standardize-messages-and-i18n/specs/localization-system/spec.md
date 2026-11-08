# Spec Delta: localization-system

## ADDED Requirements

### Requirement: Channel-Aware Message Delivery and Prefix Enforcement
The system SHALL route all player-facing messages through channel-aware delivery methods where chat feedback messages prepend the localized `general.prefix`, while action bar notifications, titles, and subtitles omit the prefix. Translation resource templates SHALL NOT embed hardcoded plugin prefix strings within localized message values.

#### Scenario: Chat message delivery with prefix
- **WHEN** the system sends a feedback or status message to a player's chat channel
- **THEN** the message is prepended with the active `general.prefix` component resolved for the player's locale.

#### Scenario: Action bar notification without prefix
- **WHEN** the system sends a notification to a player's action bar (e.g. watering, fertilizing, or clearance warnings)
- **THEN** the message is rendered directly without prepending `general.prefix`.

#### Scenario: Screen title and subtitle delivery without prefix
- **WHEN** the system renders a title or subtitle for a player (e.g. recipe discovery fanfare)
- **THEN** the title components are localized using catalog keys and displayed without prepending `general.prefix`.

#### Scenario: Elimination of hardcoded prefixes in templates
- **WHEN** a translation resource file is loaded
- **THEN** translation keys (including `prompt.*`) contain only the core message body and styles without baked-in `[Croppy]` or prefix tokens.

### Requirement: Interactive MiniMessage Tagging and Visual Formatting Standards
The system SHALL format all localized messages and command outputs following standard visual styling conventions: errors SHALL use `<red>` ending with a period (`.`), successes SHALL use `<green>` ending with an exclamation mark (`!`), list headers SHALL follow `<gold><bold>=== <Title> (<count>) ===</bold></gold>`, and actionable commands or list items SHALL embed `<click>` and `<hover>` tags.

#### Scenario: Interactive command suggestion rendering
- **WHEN** a message references an executable command (e.g. `/croppy pack build` or `/croppy recipes`)
- **THEN** the rendered component contains `<click:run_command:...>` and `<hover:show_text:...>` allowing one-click execution from chat.

#### Scenario: Interactive prompt option rendering
- **WHEN** an interactive prompt presents an option (such as `'cancel'`)
- **THEN** the option embeds a click action and hover tooltip in the player's chat.

#### Scenario: Error message formatting standard
- **WHEN** an error or validation failure message is rendered
- **THEN** the text is styled in `<red>` and terminates with a single period (`.`).

#### Scenario: Success message formatting standard
- **WHEN** an action completes successfully
- **THEN** the text is styled in `<green>` and terminates with an exclamation mark (`!`), utilizing inverted opening exclamation marks (`¡`) in Spanish locales.

### Requirement: Cloud Command Framework Exception Localization
The system SHALL register an exception controller within the Cloud 2.0 command manager to intercept command execution failures (including unauthorized access, invalid command syntax, and argument parsing errors) and format them using `PaperMessageService` with `general.prefix`.

#### Scenario: Intercepting unauthorized command execution
- **WHEN** a player executes a command without the required permission node
- **THEN** the exception controller intercepts the failure and sends `general.no-permission` prepended with `general.prefix`.

#### Scenario: Intercepting invalid command syntax
- **WHEN** a player executes a `/croppy` command with invalid or unrecognized syntax
- **THEN** the exception controller sends a localized, prefixed message describing proper command usage.
