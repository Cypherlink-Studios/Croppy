# Spec Delta

## Purpose

Provides automated remote release checking, semantic version evaluation, channel filtering, cryptographic integrity verification, and safe update staging for the Croppy Paper plugin.

## ADDED Requirements

### Requirement: Release Discovery and Channel Filtering
The system SHALL query the official GitHub repository releases API asynchronously off the main server tick to discover available releases, filtering candidates according to the configured update channel (`STABLE` or `BETA`).

#### Scenario: Stable channel queries latest production release
- **WHEN** the system queries for updates with channel configured as `STABLE`
- **THEN** the system targets the `/releases/latest` endpoint, ignoring pre-release and draft assets.

#### Scenario: Beta channel queries all releases
- **WHEN** the system queries for updates with channel configured as `BETA`
- **THEN** the system inspects available releases including pre-releases (such as `-alpha`, `-beta`, or `-rc`), selecting the most recent version tag.

#### Scenario: In-memory release caching
- **WHEN** an update check is executed within 15 minutes of a previous successful check
- **THEN** the system returns the cached release metadata without issuing a new remote HTTP request.

### Requirement: Semantic Version Evaluation
The system SHALL parse semantic versions from release tags (`v<major>.<minor>.<patch>[-<prerelease>]`) and evaluate whether the remote version is strictly newer than the currently executing plugin version.

#### Scenario: Newer version available
- **WHEN** the remote release version tag is `v0.5.14` and the running plugin version is `0.5.13`
- **THEN** the system determines that an update is available.

#### Scenario: Plugin is up to date
- **WHEN** the remote release version tag is `v0.5.13` and the running plugin version is `0.5.13`
- **THEN** the system determines that the plugin is running the latest version.

#### Scenario: Local version is ahead
- **WHEN** the running plugin version is a development build newer than the remote release
- **THEN** the system reports that no update is needed.

### Requirement: Cryptographic Integrity Verification
The system SHALL download and verify the SHA-256 hash of the target distribution JAR against the release's `checksums-sha256.txt` manifest before finalizing any update stage.

#### Scenario: Checksum matches downloaded binary
- **WHEN** the computed SHA-256 hash of the downloaded JAR matches the hash specified in `checksums-sha256.txt`
- **THEN** the system accepts the binary as authentic and proceeds to staging.

#### Scenario: Checksum mismatch or corrupted download
- **WHEN** the computed SHA-256 hash differs from the manifest entry
- **THEN** the system immediately deletes the temporary download, aborts the update process, and emits a localized error notification.

### Requirement: Safe Paper Update Staging
The system SHALL stage validated update binaries into Paper's standard `plugins/update/` directory named to match the currently running plugin JAR file, enabling automatic atomic replacement upon the next server reboot.

#### Scenario: Staging update binary
- **WHEN** update application is triggered and validation passes
- **THEN** the system writes the new JAR file into `plugins/update/<currentJarName>` and informs the administrator that a server restart is required.

#### Scenario: Overwriting existing staged update
- **WHEN** an update binary already exists in `plugins/update/`
- **THEN** the system safely replaces the existing staged binary with the newly validated JAR.

### Requirement: Passive Boot Check and Join Notifications
The system SHALL support an asynchronous update check during server startup when enabled, and notify connecting players with permission `croppy.admin.update` if an update is available.

#### Scenario: Boot check logs update availability
- **WHEN** the server starts with `updates.check-on-startup` set to true and an update is discovered
- **THEN** the system logs an informative message to the server console indicating the new version.

#### Scenario: Admin player receives login notification
- **WHEN** an update has been discovered and a player with permission `croppy.admin.update` joins the server
- **THEN** the system sends a private MiniMessage notification with a clickable prompt to run `/croppy update apply`.
