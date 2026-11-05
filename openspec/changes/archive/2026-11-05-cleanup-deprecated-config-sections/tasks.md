# Tasks

## 1. Configuration Resource and Model Modernization

- [x] 1.1 Update `croppy-paper/src/main/resources/config.yml` to remove deprecated `inspection-tool:` and `messages:` string blocks, introduce root-level `locale.default: "en-US"`, and verify valid YAML structure.
- [x] 1.2 Refactor `PluginConfig.java` to store and expose typed properties (`growthTickInterval`, `growthBaseProgress`, `growthWaterSearchRadius`, `storageType`, `defaultLocale`), remove dead inspection tool and message formatting methods, and verify compilation.

## 2. Engine Integration and Dead Code Removal

- [x] 2.1 Update `CroppyPlugin.java` to utilize `PluginConfig` typed accessors and support dual-fallback resolution for default locale (`locale.default` falling back to `messages.default-locale`), verifying that tests and runtime startup resolve the configured locale.
- [x] 2.2 Remove dead `CropInspectorService.createInspectionTool()` and simplify `InspectionToolListener.java` to delegate exclusively to `PaperMessageService`, verifying that crop inspection interactions remain functional.

## 3. Testing and Documentation

- [x] 3.1 Create and update unit tests for `PluginConfig` and command execution verifying that typed configuration defaults and fallback keys load correctly and `./gradlew test` passes with zero failures.
- [x] 3.2 Update `docs/en/configuration.md` and `docs/es/configuration.md` to document the modernized `config.yml` layout (`growth`, `storage`, `locale`) and provide cross-references to `items/` and `locales/`.

## Workflow follow-up

- Archive the change after implementation and test verification.
