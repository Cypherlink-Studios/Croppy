# Spec Delta

## MODIFIED Requirements

### Requirement: Configuration Hot Reload
The system SHALL support reloading configuration settings (`growth`, `storage`, `locale`, and `logging` blocks in `config.yml`), YAML crop definitions, item definitions across all categories, and localization resource files at runtime via `/croppy reload`, resetting any temporary runtime debug channel overrides back to `config.yml`.

#### Scenario: Successful configuration reload
- **WHEN** an administrator runs `/croppy reload`
- **THEN** the system re-reads `config.yml`, reloads all crop and item definitions from disk, atomically refreshes the localization message catalog, resets runtime debug channels to the values declared in `config.yml`, logs a concise reload summary, and sends a localized confirmation message.

#### Scenario: Failed reload due to syntax error
- **WHEN** an administrator runs `/croppy reload` with an invalid definition YAML file or corrupted locale file
- **THEN** the error is logged to console and an error notification is sent to the sender while preserving existing valid registrations and catalogs.

## ADDED Requirements

### Requirement: Runtime Debug Channel Management
The system SHALL provide `/croppy debug` administrative subcommands (`status`, `toggle <channel>`, `enable <channel>`, `disable <channel>`, `all`, `off`) restricted to permission `croppy.admin` to inspect and toggle active console debug channels at runtime without restarting the server.

#### Scenario: Querying active debug configuration
- **WHEN** an authorized administrator executes `/croppy debug status`
- **THEN** the system responds with a localized MiniMessage summary displaying the active base log level, visual style, and currently enabled debug channels with clickable toggle controls

#### Scenario: Toggling a specific debug channel
- **WHEN** an authorized administrator executes `/croppy debug toggle growth` or `/croppy debug enable packet`
- **THEN** the system updates the active runtime channel set immediately so matching `DEBUG`/`TRACE` logs emit to the server console only, and sends localized confirmation feedback to the sender

#### Scenario: Enabling or disabling all debug channels
- **WHEN** an authorized administrator executes `/croppy debug all` or `/croppy debug off`
- **THEN** the system enables or clears all runtime debug channel overrides immediately and notifies the sender with a localized confirmation

#### Scenario: Rejecting unknown debug channel name
- **WHEN** an administrator passes an unrecognized channel name to `/croppy debug toggle <channel>`
- **THEN** the system rejects the command and sends a localized invalid-channel error message listing valid channel identifiers
