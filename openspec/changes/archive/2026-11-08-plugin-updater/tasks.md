# Tasks

## 1. Domain API & Semantic Versioning (`croppy-api`)

- [x] 1.1 Create `SemanticVersion` record supporting `<major>.<minor>.<patch>[-<prerelease>]` parsing and `Comparable<SemanticVersion>` ordering, and verify with unit tests covering version precedence and pre-release tags.
- [x] 1.2 Create `UpdateChannel` enum (`STABLE`, `BETA`), `UpdateCheckResult`, and `UpdateApplyResult` immutable records in package `dev.darkblade.croppy.api.update`.
- [x] 1.3 Create `UpdateService` port interface in `croppy-api` defining asynchronous contracts for checking updates and applying staged updates.

## 2. Remote Client, Checksum & Staging Engine (`croppy-paper`)

- [x] 2.1 Implement `GitHubReleaseClient` in `croppy-paper` utilizing Java 21 `HttpClient` to query GitHub Releases API, filter by release channel, handle rate-limit responses, and cache results with a 15-minute TTL; verify with unit tests using mock HTTP responses.
- [x] 2.2 Implement `ChecksumValidator` to parse `checksums-sha256.txt` manifests and verify the SHA-256 digest of downloaded binaries; verify with unit tests covering valid, corrupted, and missing entries.
- [x] 2.3 Implement `PaperUpdateService` to orchestrate release checking, off-tick downloading into a temporary `.tmp` file, checksum validation, and atomic staging to `plugins/update/<currentJarName>`.

## 3. Configuration & Localization

- [x] 3.1 Add `updates` section (`enabled`, `check-on-startup`, `channel`) to `croppy-paper/src/main/resources/config.yml` and bind parsing in `PluginConfig.java`.
- [x] 3.2 Add update localization message keys in `croppy-paper/src/main/resources/locales/en-US.yml` and `es-ES.yml` covering checking, up-to-date, update-available, downloading, checksum verification, error handling, and restart instructions.

## 4. Commands, Lifecycle & Admin Notifications

- [x] 4.1 Register `@Command("croppy update check")` and `@Command("croppy update apply")` in `CroppyCommands.java` requiring permission `croppy.admin.update` with clickable MiniMessage feedback; verify with Cloud 2.0 command test cases.
- [x] 4.2 Implement asynchronous boot check in `CroppyPlugin` and register `UpdateNotificationListener` to notify administrators on player join when an update is available.
- [x] 4.3 Run pre-commit test verification gate with `./gradlew.bat test` to verify all domain, command, and adapter tests pass with 0 failures.

## Workflow follow-up

- Sync delta specs to durable specifications in `openspec/specs/` and archive the change.
- Record architectural milestone in Engram persistent memory (`mem_save`).
