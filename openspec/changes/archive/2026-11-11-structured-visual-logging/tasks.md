# Tasks

## 1. Pure Domain Logging Port (`croppy-api`)

- [x] 1.1 Create `LogLevel`, `LogChannel`, and `CroppyLogger` in `dev.darkblade.croppy.api.logging` supporting severity ordering, case-insensitive channel resolution, lazy `Supplier<String>` debug/trace overloads, throwable logging, and `scoped(String)` child loggers; verify `croppy-api` remains dependency-free (`java.*` only) via `./gradlew :croppy-api:compileJava`.
- [x] 1.2 Implement `NoOpCroppyLogger` and `JulCroppyLogger` in `dev.darkblade.croppy.api.logging`, and add `default CroppyLogger getCroppyLogger()` on `AddonContext` while preserving `Logger getLogger()` compatibility; verify compilation against external addon contracts.
- [x] 1.3 Add JUnit 5 unit tests in `croppy-api` covering `LogLevel` thresholds, `LogChannel.fromString`, scoped logger delegation, lazy supplier suppression, and `JulCroppyLogger` severity mapping; verify with `./gradlew :croppy-api:test`.

## 2. Core & Integration Migration (`croppy-core`, `croppy-integrations`)

- [x] 2.1 Migrate `CroppyCore`, `DefaultAddonManager`, `YamlAddonConfig`, and `DefaultResourcePackCompiler` to `CroppyLogger` (retaining JUL compatibility overloads where needed), removing static `Logger.getLogger(...)` fields and ensuring existing `croppy-core` tests pass via `./gradlew :croppy-core:test`.
- [x] 2.2 Migrate all YAML loaders (`YamlSpeciesLoader`, `YamlItemLoader`, `YamlFertilizerLoader`, `YamlLocaleLoader`, `YamlMenuLoader`, `YamlProcessLoader`, `YamlRecipeLoader`, `YamlStationLoader`, `YamlInteractionLoader`, `YamlRuleLoader`, `YamlCraftingRecipeLoader`, `YamlAddonDescriptionLoader`) to `CroppyLogger`, demoting per-entry loading logs to `DEBUG` (`LogChannel.LOADER`) and eliminating `YamlItemLoader`'s static logger fallback; verify with `croppy-core` loader unit tests.
- [x] 2.3 Migrate `IntegrationManager`, `WorldGuardIntegration`, `WorldGuardFlags`, `PapiIntegration`, and `VaultIntegration` to `CroppyLogger` (`LogChannel.INTEGRATION`), preserving JUL compatibility constructors for tests if needed, and verify with `./gradlew :croppy-integrations:common:test :croppy-integrations:worldguard:test :croppy-integrations:papi:test :croppy-integrations:vault:test`.

## 3. Logging Configuration & Paper Logger Adapter (`croppy-paper`)

- [x] 3.1 Add `logging.level` (`INFO`), `logging.style` (`RICH`), and `logging.debug-channels` (`[]`) to `croppy-paper/src/main/resources/config.yml` and `PluginConfig` (with `LogStyle` enum and safe fallback defaults for invalid values); verify with expanded `PluginConfigTest`.
- [x] 3.2 Implement `PaperCroppyLogger` in `dev.darkblade.croppy.platform.paper.logging` backed by `ComponentLogger`, safe `Component.text(...)` payload rendering, thread-safe level/channel state, zero-allocation lazy supplier filtering, warning/error tracking, and runtime debug channel controls; verify with unit tests for filtering, supplier skipping, scoping, and `applyConfig` resets.

## 4. Lifecycle Visual Reporter, Addon JUL Bridge & Service Migration (`croppy-paper`)

- [x] 4.1 Implement `ConsoleLifecycleReporter` supporting `RICH` (MiniMessage green-gradient ASCII banner, phase-streamed rows with millisecond timings, status badges, and warning/error footer via `ConsoleCommandSender`/`ComponentLogger`) and `COMPACT` (single-line `[Croppy]` summaries); verify formatting behavior for both modes and warning counters via unit tests.
- [x] 4.2 Update `CroppyPlugin` (`onLoad`, `onEnable`, `reloadPlugin`, `onDisable`) to initialize early logging on load, stream enable phases, emit concise reload/shutdown summaries, and reset runtime debug channels to `config.yml` on reload; migrate `croppy-paper` platform services, storage, resource pack, updater, renderers, trackers, and listeners from `java.util.logging.Logger` to `CroppyLogger` and verify affected tests pass.
- [x] 4.3 Implement the isolated JUL-to-`CroppyLogger` `Handler` bridge (`useParentHandlers = false`) and `getCroppyLogger()` override in `PaperAddonContext`, and update `PaperAddonManager` and `AddonDecommissioner` to route addon YAML loading to `DEBUG` and report consolidated addon startup summaries; verify with `PaperAddonContextTest`, `PaperAddonManagerTest`, and `AddonDecommissionerTest`.

## 5. Silent Exception Instrumentation & `/croppy debug` Command (`croppy-paper`)

- [x] 5.1 Replace silent `catch (Exception ignored)` blocks in `PaperStationTicker`, `StationViewerTracker`, `StationBlockListener`, `StationInteractListener`, and `CropTargetResolver` (plus existing `Level.FINE` calls in packet/resource-pack services) with lazy `TRACE`/`DEBUG` `CroppyLogger` calls on their subsystem channels; verify station, display, and raycast tests pass.
- [x] 5.2 Add `/croppy debug <status|toggle|enable|disable|all|off>` subcommands in `CroppyCommands` (`@Permission("croppy.admin")`, Cloud channel suggestions, console-only debug emission) and add synchronized MiniMessage keys (`command.debug.*`) to `locales/en-US.yml` and `locales/es-ES.yml`; verify with `CroppyCommandsTest`, `CroppyCommandsExecutionTest`, and `LocaleResourcesIntegrityTest`.

## 6. Final Verification

- [x] 6.1 Run `.\gradlew.bat test` and `.\gradlew.bat build` across the full repository and confirm zero compilation errors and 100% passing unit tests.
- [x] 6.2 Run `openspec validate "structured-visual-logging" --strict` and verify all OpenSpec artifacts pass strict validation.
