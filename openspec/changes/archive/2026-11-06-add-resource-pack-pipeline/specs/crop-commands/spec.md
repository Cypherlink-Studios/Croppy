# Spec Delta: crop-commands

## ADDED Requirements

### Requirement: Resource Pack Management Commands
The system SHALL provide administrative commands under `/croppy pack` to build the unified resource pack from discovered addon assets and dispatch the pack to connected players, requiring permission node `croppy.admin.pack`.

#### Scenario: Building the unified resource pack
- **WHEN** an administrator runs `/croppy pack build`
- **THEN** the system triggers asynchronous compilation of the resource pack, writes `croppy-unified-pack.zip` to the exports directory, updates the active SHA-1 checksum in memory, and outputs compilation time, byte size, and asset count formatted with MiniMessage

#### Scenario: Dispatching resource pack to target player
- **WHEN** an administrator runs `/croppy pack send <player>` with a valid online player name
- **THEN** the system sends the active resource pack prompt to that player and notifies the command sender

#### Scenario: Dispatching resource pack to all players from console
- **WHEN** the server console runs `/croppy pack send` without specifying a player
- **THEN** the system dispatches the resource pack prompt to all online players and outputs confirmation to the console

#### Scenario: Unauthorized pack command execution
- **WHEN** a player lacking permission `croppy.admin.pack` executes `/croppy pack build` or `/croppy pack send`
- **THEN** the system denies execution and returns a localized unauthorized error message
