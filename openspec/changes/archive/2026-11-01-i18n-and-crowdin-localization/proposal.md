# Proposal

## Why

Croppy's player-facing chat notifications, command feedback, action bars, inspector cards, and interactive prompts are currently fragmented across configuration files and hardcoded string literals (including Spanish literals in Java classes). This violates `AGENTS.md` architectural conventions, prevents multi-language player support, and requires code changes for new translations. Establishing a decoupled, production-ready internationalization (i18n) infrastructure with Crowdin integration and automated CI/CD validation resolves this technical debt, enabling dynamic per-player localization and community-driven translations.

## What Changes

- **Pure API Localization Contracts (`croppy-api`)**: Introduce `MessageKey`, `LocaleRef`, and `TranslationProvider` without introducing any external or platform dependencies.
- **Domain Localization Engine & Catalog (`croppy-core`)**: Implement `YamlLocaleLoader` using SnakeYAML to load hierarchical YAML translation bundles, `DefaultMessageCatalog` with thread-safe atomic cache replacement, fallback resolution chain (`player locale -> language fallback -> server default -> source locale -> safe token fallback`), and `LocaleValidationReport` to detect missing or malformed keys.
- **Paper Adapter & Adventure MiniMessage Rendering (`croppy-paper`)**: Implement `PaperMessageService` bridging Adventure audiences and MiniMessage formatting, automatic player locale detection via `Player#locale()`, argument sanitization against MiniMessage injection exploits, and integration with `CroppyPlugin` lifecycle.
- **Content Localization Fallback Strategy**: Support localization of custom items, species, recipes, and stations through locale overrides (`content.<category>.<id>.name|lore`) falling back cleanly to content YAML default display names.
- **Crowdin & CI/CD Automation**: Provide `crowdin.yml` mapping source file (`locales/en-US.yml`) to translated bundles (`locales/%locale%.yml`), automated GitHub Actions build and validation workflows, and pre-commit Gradle test gates.
- **Migration of Existing Strings**: Externalize all hardcoded English and Spanish literals across commands, listeners (`WateringListener`, `FertilizerListener`, `StationInteractListener`), `CropInspectorService`, and `ChatPromptService` into Standard English source `en-US.yml` and secondary `es-ES.yml`.

## Capabilities

### New Capabilities
- `localization-system`: Core internationalization and localization infrastructure covering YAML locale loading, in-memory message catalogs, deterministic fallback chains, MiniMessage argument rendering, content translation overrides, automated resource validation, and Crowdin integration.

### Modified Capabilities
- `crop-commands`: Modify command feedback to resolve localized messages per sender locale and extend `/croppy reload` to reload localization message catalogs atomically at runtime.

## Impact

- **Codebases Affected**: `croppy-api`, `croppy-core`, `croppy-paper`.
- **APIs**: New interfaces in `dev.darkblade.croppy.api.i18n` (`MessageKey`, `LocaleRef`, `TranslationProvider`).
- **Dependencies**: No new runtime dependencies. Reuses existing SnakeYAML in core and Kyori Adventure in paper.
- **Workflows & Repository**: Adds `crowdin.yml` at repository root, `.github/workflows/build.yml` and `.github/workflows/crowdin-sync.yml`.
- **Breaking Changes**: None. Backwards-compatible fallback retains content definition display names when translations are absent.
