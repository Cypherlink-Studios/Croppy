# Proposal

## Why

Croppy's current logging relies on raw `java.util.logging.Logger` instances passed across `croppy-core`, `croppy-integrations`, and `croppy-paper`, producing 40–60+ flat, unformatted `INFO` lines during startup and reload with inconsistent granularity across YAML loaders. Several core utilities and addon contexts also instantiate static or detached `Logger.getLogger(...)` instances that bypass plugin context, while hot paths contain silent `catch (Exception ignored)` blocks without configurable debug channels or Paper Adventure styling.

Introducing a pure domain logging port with channel-based filtering and a Paper console renderer resolves both the structural fragmentation and console readability issues, giving administrators clean phase-grouped lifecycle dashboards (`RICH` and `COMPACT` styles) and zero-allocation runtime debug diagnostics.

## What Changes

- **Pure Domain Logging Port (`croppy-api`)**: Introduce `CroppyLogger`, `LogLevel`, and `LogChannel` in pure Java 21, including lazy `Supplier<String>` overloads for zero-overhead `debug` and `trace` emission and scoped child loggers.
- **Addon Context Compatibility Bridge**: Retain `AddonContext.getLogger()` (`java.util.logging.Logger`) backed by a JUL bridge forwarding records to a scoped `[Addon:<id>]` `CroppyLogger`, while adding a default `getCroppyLogger()` method on `AddonContext` for zero-breaking compatibility with existing addons and tests.
- **Core, Loader & Integration Migration (`croppy-core`, `croppy-integrations`)**: Migrate `CroppyCore`, YAML loaders (`YamlSpeciesLoader`, `YamlItemLoader`, `YamlFertilizerLoader`, `YamlLocaleLoader`, etc.), `DefaultResourcePackCompiler`, `YamlAddonConfig`, and `IntegrationManager`/integrations to `CroppyLogger` (with temporary JUL constructor/method overloads where appropriate), demoting per-file loader messages to `DEBUG` (`LogChannel.LOADER`) and eliminating orphaned static `Logger.getLogger(...)` calls.
- **Paper Console Renderer & Lifecycle Dashboards (`croppy-paper`)**: Implement `PaperCroppyLogger` backed by Paper's `ComponentLogger` (for thread-safe runtime/async logs with native ANSI/file output) and `ConsoleCommandSender` (for multi-line visual headers/footers), supporting two configurable styles:
  - `RICH` (default): MiniMessage green-gradient ASCII banner, phase-streamed startup summary with millisecond timings, integration/addon status badges, and warning/error summary footer; concise reload and shutdown summaries.
  - `COMPACT`: Clean single-line `[Croppy]` prefixed output without large multi-line ASCII banners.
- **Silent Exception & Hot-Path Instrumentation**: Replace silent `catch (Exception ignored)` blocks in tickers, virtual displays, target raytracing, and station listeners with `TRACE`/`DEBUG` channel logs.
- **Logging Configuration & `/croppy debug` Command**: Add a `logging` section (`level`, `style`, `debug-channels`) in `config.yml` and `PluginConfig`, add `/croppy debug <status|toggle|enable|disable|all|off>` in `CroppyCommands` (directing debug output exclusively to console, with localized MiniMessage command feedback in `en-US.yml` and `es-ES.yml`), and reset runtime debug channel overrides to `config.yml` state upon `/croppy reload`.

## Capabilities

### New Capabilities
- `structured-logging`: Pure Java 21 domain logging abstraction (`CroppyLogger`, `LogLevel`, `LogChannel`), lazy debug/trace evaluation, channel-scoped filtering, JUL bridge compatibility, `logging.*` configuration, and Paper Adventure console rendering (`RICH` and `COMPACT` styles).

### Modified Capabilities
- `plugin-lifecycle`: Enhance platform lifecycle hooks (`onLoad`, `onEnable`, `reloadPlugin`, `onDisable`) to initialize early scoped logging and emit structured phase-streamed startup, reload, and shutdown summaries with timing and warning diagnostics.
- `addon-system`: Bridge `AddonContext.getLogger()` JUL records into scoped `[Addon:<id>]` channels, expose default `getCroppyLogger()` access on `AddonContext`, and consolidate addon discovery/enable reporting in `PaperAddonManager`.
- `crop-commands`: Add `/croppy debug` subcommands for runtime debug channel management (console-only output) and ensure `/croppy reload` synchronizes logging configuration and resets active runtime debug channel overrides back to `config.yml`.

## Impact

- **Affected Modules & Packages**:
  - `croppy-api`: New logging types under `dev.darkblade.croppy.api.logging` (`CroppyLogger`, `LogLevel`, `LogChannel`); `AddonContext` default method.
  - `croppy-core`: `CroppyCore`, `DefaultAddonManager`, `YamlAddonConfig`, `DefaultResourcePackCompiler`, and all YAML content loaders (`Yaml*Loader`).
  - `croppy-integrations`: `IntegrationManager`, `WorldGuardIntegration`, `WorldGuardFlags`, `PapiIntegration`, `VaultIntegration`.
  - `croppy-paper`: New logging adapter/reporter under `dev.darkblade.croppy.platform.paper.logging`, `CroppyPlugin`, `PluginConfig`, `config.yml`, `CroppyCommands`, `locales/en-US.yml`, `locales/es-ES.yml`, `PaperAddonManager`, `PaperAddonContext`, tickers, renderers, storage, and listeners.
- **Dependencies**: Zero new third-party dependencies (`croppy-api` remains pure Java 21; `croppy-core` remains pure Java 21 + SnakeYAML; `croppy-paper` uses Paper 1.21.8 `ComponentLogger` and Adventure).
- **Compatibility**: Preserves `AddonContext.getLogger()` return type (`java.util.logging.Logger`) and existing public constructors via compatibility adapters/overloads so external addons (e.g., `Croppy-addons`) continue compiling and running without modification.
