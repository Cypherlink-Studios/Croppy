# Spec Delta

## ADDED Requirements

### Requirement: Plugin Update Administrative Commands
The system SHALL provide administrative commands under `/croppy update <check | apply>` with Cloud 2.0 dispatch, requiring permission node `croppy.admin.update`.

#### Scenario: Check for updates via command
- **WHEN** an authorized administrator runs `/croppy update check`
- **THEN** the system triggers an asynchronous release check and displays real-time version status, target release channel, and a clickable update prompt if a newer version is available.

#### Scenario: Apply available update via command
- **WHEN** an authorized administrator runs `/croppy update apply` while a newer version exists
- **THEN** the system asynchronously downloads, validates the SHA-256 checksum, stages the JAR in `plugins/update/`, and outputs localized progress and restart instructions.

#### Scenario: Apply update when already on latest version
- **WHEN** an administrator runs `/croppy update apply` and the plugin is already at or ahead of the latest remote version
- **THEN** the system terminates the command immediately with a message stating that no update is required.

#### Scenario: Unauthorized player update command execution
- **WHEN** a player lacking permission `croppy.admin.update` runs `/croppy update check` or `/croppy update apply`
- **THEN** the system denies execution with a standard permission error message.
