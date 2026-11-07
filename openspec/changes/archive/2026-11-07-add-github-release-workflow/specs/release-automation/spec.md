# Spec Delta

## Purpose

Automates the validation, packaging, cryptographic verification, and GitHub Release publication of Croppy Paper plugin binaries, developer API JARs, and companion resource packs.

## ADDED Requirements

### Requirement: Version Consistency Verification
The release automation SHALL verify that the release tag version matches the project version declared in `build.gradle.kts` before executing build or publishing steps, failing fast on mismatch.

#### Scenario: Matching version tag
- **WHEN** a release workflow triggers on git tag `v0.5.9` and `build.gradle.kts` defines `version = "0.5.9"`
- **THEN** version verification succeeds and the release pipeline proceeds

#### Scenario: Mismatched version tag
- **WHEN** a release workflow triggers on git tag `v0.6.0` but `build.gradle.kts` defines `version = "0.5.9"`
- **THEN** version verification fails, emits an error annotation, and aborts the workflow execution

### Requirement: Automated Test Verification Gate
The release automation SHALL execute project test suites using Gradle prior to compiling distribution packages, aborting if any verification fails.

#### Scenario: Test suite execution succeeds
- **WHEN** the test verification gate executes `./gradlew check test` and all suites pass
- **THEN** the pipeline continues to artifact compilation and packaging

#### Scenario: Test suite failure aborts release
- **WHEN** any unit or integration test suite fails during `./gradlew check test`
- **THEN** the workflow terminates immediately without assembling release artifacts or publishing a release

### Requirement: Multi-Module Distribution Artifact Assembly
The release automation SHALL compile and package the Paper runtime plugin (`Croppy-<version>.jar`), developer API library (`croppy-api-<version>.jar`), and companion resource pack archive (`Croppy-ResourcePack-<version>.zip`).

#### Scenario: Distribution artifacts packaged
- **WHEN** artifact assembly executes for version `0.5.9`
- **THEN** `Croppy-0.5.9.jar` is produced from `croppy-paper:shadowJar`, `croppy-api-0.5.9.jar` is produced from `croppy-api:jar`, and `Croppy-ResourcePack-0.5.9.zip` is created containing `pack.mcmeta` at archive root

### Requirement: Cryptographic Integrity Verification
The release automation SHALL generate SHA-256 and SHA-1 checksum manifests for all compiled distribution binaries and resource pack archives.

#### Scenario: Checksum files generated
- **WHEN** release assets are staged in the workspace
- **THEN** `checksums-sha256.txt` and `checksums-sha1.txt` are created containing valid hexadecimal hashes for all staged binaries

### Requirement: GitHub Release Publication
The release automation SHALL publish a GitHub Release under the corresponding version tag, attaching all staged JARs, resource pack ZIP, and checksum manifests with automated release notes.

#### Scenario: Automated GitHub Release creation
- **WHEN** all build, packaging, and checksum stages complete successfully
- **THEN** a GitHub Release is published with title `Croppy v<version>`, populated release notes, and all 5 staged asset files attached

#### Scenario: Pre-release tag detection
- **WHEN** a git tag contains pre-release qualifiers such as `-alpha`, `-beta`, or `-rc`
- **THEN** the published GitHub Release is automatically flagged as a pre-release
