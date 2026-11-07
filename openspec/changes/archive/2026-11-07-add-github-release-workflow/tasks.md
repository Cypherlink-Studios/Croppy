# Tasks

## 1. GitHub Actions Release Workflow Definition

- [x] 1.1 Create `.github/workflows/release.yml` declaring `push: tags: ['v*']`, `workflow_dispatch` (with `tag_name`, `draft`, and `prerelease` inputs), and `contents: write` permissions.
- [x] 1.2 Implement runner environment setup steps including repository checkout with `fetch-depth: 0`, Temurin JDK 21 setup, Gradle cache configuration, and executable permissions for `gradlew`.
- [x] 1.3 Implement version and tag resolution with fail-fast safety check comparing `./gradlew properties --property version` against the target release tag.
- [x] 1.4 Configure test verification gate executing `./gradlew check test --info` prior to artifact compilation.
- [x] 1.5 Implement build and staging steps for `Croppy-<version>.jar` (from `:croppy-paper:shadowJar`) and `croppy-api-<version>.jar` (from `:croppy-api:jar`).
- [x] 1.6 Implement packaging step for `Croppy-ResourcePack-<version>.zip` ensuring `pack.mcmeta` and assets reside at the archive root.
- [x] 1.7 Implement cryptographic checksum generation generating `checksums-sha256.txt` and `checksums-sha1.txt` for all staged artifacts.
- [x] 1.8 Configure GitHub Release publication step using `softprops/action-gh-release@v2` attaching all 5 release assets with automated release notes.

## 2. Validation & Verification

- [x] 2.1 Validate `.github/workflows/release.yml` YAML syntax and structure against GitHub Actions schemas.
- [x] 2.2 Verify local Gradle version extraction command and artifact path resolution behave identically to workflow script logic.
