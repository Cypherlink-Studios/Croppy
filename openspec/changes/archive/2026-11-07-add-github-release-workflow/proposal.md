# Proposal

## Why

Currently, Croppy relies on manual artifact building and packaging for server distributions and releases. Server administrators and addon developers require consistent, automated, and verified distributions whenever new versions or git tags are pushed, including compiled Paper plugin jars, developer API jars, companion resource pack archives, and cryptographic checksums for server deployment. Automating this process with strict version validation prevents human error, release desynchronization, and ensures seamless delivery of assets.

## What Changes

- Add GitHub Actions release workflow (`.github/workflows/release.yml`) triggered on tag pushes (`v*`) and manual triggers (`workflow_dispatch`).
- Implement version consistency safety check validating that the triggering tag matches the project version configured in `build.gradle.kts`.
- Execute automated verification test suites (`./gradlew check test`) prior to release packaging.
- Compile and assemble the distribution artifacts:
  - Main Paper plugin (`Croppy-<version>.jar` via `shadowJar` relocation).
  - Addon developer API (`croppy-api-<version>.jar`).
  - Companion Resource Pack (`Croppy-ResourcePack-<version>.zip` packaged from `resourcepack/` root).
- Generate SHA-256 and SHA-1 checksum files (`checksums-sha256.txt` and `checksums-sha1.txt`) for all release binaries and packs.
- Publish GitHub Release via `softprops/action-gh-release@v2` with automated release notes and configurable draft / pre-release flags.

## Capabilities

### New Capabilities
- `release-automation`: Automated build, packaging, checksumming, version integrity validation, and GitHub Release deployment of Croppy binaries and companion assets.

### Modified Capabilities
<!-- None: No existing domain behavior requirements are changing. -->

## Impact

- **Affected Code/Files**: Adds `.github/workflows/release.yml`.
- **Dependencies**: Uses `actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`, and `softprops/action-gh-release@v2`.
- **Compatibility**: Requires Java 21 Temurin runner; does not modify runtime Java code or Paper server APIs.
