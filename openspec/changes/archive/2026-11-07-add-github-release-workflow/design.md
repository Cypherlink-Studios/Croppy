# Design

## Context

Croppy is a modular Paper 1.21.8+ simulation engine with multi-project Gradle setup (`croppy-api`, `croppy-core`, `croppy-paper`, and `croppy-integrations`). Currently, builds are verified via `.github/workflows/build.yml`, but distribution releases require manual building and packaging. See `proposal.md` for background motivation.

## Goals / Non-Goals

**Goals:**
- Provide a robust, unattended GitHub Actions release workflow triggered automatically by version tags (`v*`) and manually on demand (`workflow_dispatch`).
- Ensure version consistency by failing fast before build if git tag and `build.gradle.kts` differ.
- Package all essential deployment assets: the Paper plugin shadow JAR, the developer API JAR, and the companion resource pack ZIP.
- Provide cryptographic manifests (SHA-256 for standard verification and SHA-1 for Minecraft `server.properties` `resource-pack-sha1`).
- Publish releases with automated changelog notes and configurable draft / pre-release settings.

**Non-Goals:**
- Maven Central or JitPack automated deployment (handled separately via repository publishing plugins if needed in future changes).
- Third-party mod/plugin portal distribution (e.g., Modrinth, Hangar, SpigotMC API publishing).
- Modifying runtime Java code or Paper plugin bootstrap logic.

## Decisions

### 1. Dual Trigger (`push: tags: ['v*']` and `workflow_dispatch`)
- **Choice**: Listen to git tag pushes matching `v*` and support `workflow_dispatch` with optional manual inputs (`tag_name`, `draft`, `prerelease`).
- **Rationale**: Tags represent immutable release milestones in git history. Manual dispatch enables testing the release pipeline or publishing a draft release without immediately tagging the main branch.
- **Alternatives Considered**: Webhook triggers or release creation directly via GitHub web UI (lacks pre-build verification and local artifact synthesis).

### 2. Version Safety Guard
- **Choice**: Extract project version directly using `./gradlew properties --property version -q` and compare against `${TAG#v}`. If mismatched, emit an action error annotation and exit with code 1.
- **Rationale**: Prevents accidental releases where git tags do not match internal Gradle version numbers, eliminating desynchronized plugin descriptors.
- **Alternatives Considered**: Overwriting the Gradle version with the git tag dynamically during CI (rejected because it bypasses source control version declarations).

### 3. Packaging Companion Resource Pack from Root
- **Choice**: Create `Croppy-ResourcePack-<version>.zip` by executing `(cd resourcepack && zip -r ...)` so `pack.mcmeta` and `pack.png` are placed at the root of the ZIP archive.
- **Rationale**: Minecraft Java requires `pack.mcmeta` at the root of the archive. Server administrators and clients can directly load this ZIP without manual extraction.
- **Alternatives Considered**: Having Gradle build the ZIP (requires adding custom Gradle tasks to root project; using native Linux runner utility keeps build scripts clean).

### 4. Dual Hashing (SHA-256 and SHA-1)
- **Choice**: Generate both `checksums-sha256.txt` and `checksums-sha1.txt` for all staged release assets.
- **Rationale**: SHA-256 is standard industry practice for security verification. Minecraft `server.properties` specifically requires SHA-1 (`resource-pack-sha1`), making SHA-1 immediately actionable for server admins.
- **Alternatives Considered**: Only generating SHA-256 (inconveniences server admins who need SHA-1 for server resource packs).

### 5. Action Selection: `softprops/action-gh-release@v2`
- **Choice**: Use `softprops/action-gh-release@v2` with `generate_release_notes: true`.
- **Rationale**: Well-maintained, widely adopted standard for GitHub Releases with robust multi-file asset uploading, automatic git release notes generation, and draft/pre-release support.
- **Alternatives Considered**: `gh release create` via GitHub CLI (less declarative, more boilerplate for draft/pre-release conditional handling).

## Risks / Trade-offs

- **[Risk]** Missing or malformed `resourcepack/pack.mcmeta` in the ZIP → **Mitigation**: Scoped zip execution `(cd resourcepack && zip -r ...)` ensures root-level placement, verified by unit tests and pack audits.
- **[Risk]** Uncommitted version bumps during release tagging → **Mitigation**: Version Safety Guard fails fast before executing tests or compilations.
- **[Risk]** Long test suite execution delaying release → **Mitigation**: Gradle caching via `gradle/actions/setup-gradle@v4` speeds up builds and dependency resolution.

## Migration Plan

1. Add `.github/workflows/release.yml`.
2. Push workflow to repository.
3. Test manual release dispatch in draft mode or tag push for next version.
