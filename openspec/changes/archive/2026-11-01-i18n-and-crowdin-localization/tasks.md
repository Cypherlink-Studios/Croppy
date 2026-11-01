# Tasks

## 1. Pure API Contracts (`croppy-api`)

- [x] 1.1 Create `MessageKey` record in `dev.darkblade.croppy.api.i18n` with namespace, value, and validation, verifying immutability and string formatting
- [x] 1.2 Create `LocaleRef` record in `dev.darkblade.croppy.api.i18n` representing normalized language and region tags with factory methods from `Locale` and String
- [x] 1.3 Create `TranslationProvider` interface in `dev.darkblade.croppy.api.i18n` defining contracts for querying templates and resolving localized strings

## 2. Core Localization Engine & Catalog (`croppy-core`)

- [x] 2.1 Implement `YamlLocaleLoader` using SnakeYAML to parse nested YAML dictionaries into flattened dot-delimited key-value mappings
- [x] 2.2 Implement `DefaultMessageCatalog` with thread-safe storage, atomic reference swapping, and deterministic fallback resolution chain
- [x] 2.3 Implement `LocaleValidator` and `LocaleValidationReport` to compare secondary locale files against source keys and detect malformed structures
- [x] 2.4 Add JUnit 5 unit tests in `croppy-core` (`YamlLocaleLoaderTest`, `DefaultMessageCatalogTest`, `LocaleValidatorTest`) verifying loading, fallbacks, and validation

## 3. Platform Message Service & MiniMessage Adapter (`croppy-paper`)

- [x] 3.1 Implement `PaperMessageService` adapting Adventure's `Audience` and `MiniMessage` with per-player `Player#locale()` resolution
- [x] 3.2 Implement argument sanitization in `PaperMessageService` to prevent MiniMessage formatting injection from untrusted arguments
- [x] 3.3 Implement content localization override resolver in `PaperMessageService` supporting `content.<category>.<id>.name|lore`
- [x] 3.4 Wire `PaperMessageService` into `CroppyPlugin` initialization, default file extraction to `plugins/Croppy/locales/`, and `/croppy reload`
- [x] 3.5 Add unit tests in `croppy-paper` (`PaperMessageServiceTest`) verifying locale selection, sanitization, and fallback rendering

## 4. Resource Bundles & Application Migration

- [x] 4.1 Create `locales/en-US.yml` under `croppy-paper/src/main/resources/locales/` as the canonical source bundle in Standard English
- [x] 4.2 Create `locales/es-ES.yml` under `croppy-paper/src/main/resources/locales/` as the bundled secondary Spanish translation
- [x] 4.3 Migrate `CroppyCommands.java` from hardcoded strings and legacy `config.yml` messages to `PaperMessageService` and `MessageKey`
- [x] 4.4 Migrate `WateringListener.java` and `FertilizerListener.java` action bar feedback to `PaperMessageService`
- [x] 4.5 Migrate `CropInspectorService.java` inspection cards, progress states, and labels to `PaperMessageService`
- [x] 4.6 Migrate `ChatPromptService.java` prompt messages, timeouts, and error alerts to `PaperMessageService`
- [x] 4.7 Migrate `StationInteractListener.java` temperature displays and empty station feedback to `PaperMessageService`

## 5. Crowdin, CI/CD & Final Verification

- [x] 5.1 Create `crowdin.yml` at repository root configuring source `en-US.yml`, target translation patterns, and file update options
- [x] 5.2 Create `.github/workflows/build.yml` running Gradle build, test suite, and locale validation on push and PR
- [x] 5.3 Create `.github/workflows/crowdin-sync.yml` configured for secure Crowdin synchronization via GitHub Secrets
- [x] 5.4 Create `docs/localization.md` documenting architecture, key naming conventions, and instructions for adding new languages
- [x] 5.5 Execute full `./gradlew test` across all subprojects, verifying that all existing tests and new localization tests pass cleanly
