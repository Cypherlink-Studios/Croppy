# Design

## Context

See `proposal.md` for motivation and scope. Architecturally, Croppy enforces strict Hexagonal boundaries (`croppy-api` zero external dependencies; `croppy-core` pure Java 21 + SnakeYAML; `croppy-paper` Paper 1.21.8 + Kyori Adventure) and exposes `AddonContext.getLogger()` (`java.util.logging.Logger`) to external addons such as `Croppy-addons` (`CroppyCoffeeAddon` and `CroppyCoffeeAddonTest` implement `AddonContext` directly). Paper 1.21.8 provides `JavaPlugin#getComponentLogger()` (`ComponentLogger`) and `Bukkit.getConsoleSender()`, both backed by Adventure/Log4j2 with native ANSI rendering in terminals and automatic tag stripping in `logs/latest.log`.

## Goals / Non-Goals

**Goals:**
- Introduce a pure Java 21 logging port (`CroppyLogger`, `LogLevel`, `LogChannel`) in `croppy-api` usable across domain, integration, and platform modules.
- Support zero-allocation lazy `Supplier<String>` evaluation on `DEBUG`/`TRACE` paths so high-frequency tickers and packet listeners pay negligible cost when disabled.
- Maintain 100% source and binary compatibility for existing addons implementing or consuming `AddonContext`.
- Stream phase-grouped `RICH` (default) or `COMPACT` console summaries during `onEnable`, `reloadPlugin`, and `onDisable` with timing metrics and warning/error tracking.
- Expose `logging.*` configuration in `config.yml` and `/croppy debug` runtime channel controls targeting console output only.

**Non-Goals:**
- Mirroring `DEBUG`/`TRACE` console logs to in-game player chat (console-only per requirements).
- Adding external log shippers, JSON file appenders, or altering third-party library internal loggers (e.g., HikariCP/Libby internals).
- Changing intentional `NumberFormatException` control-flow parsing blocks in expression evaluators.

## Decisions

### 1. Pure Domain Logging Port in `croppy-api`
Define `CroppyLogger`, `LogLevel`, `LogChannel`, and lightweight pure-Java implementations (`NoOpCroppyLogger`, `JulCroppyLogger`) under `dev.darkblade.croppy.api.logging` using only `java.util.logging` and standard Java 21 types.
- **LogLevel**: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` ordered by verbosity (`level.isEnabled(threshold)`).
- **LogChannel**: `CORE`, `LOADER`, `GROWTH`, `STATION`, `DISPLAY`, `PACKET`, `STORAGE`, `ADDON`, `PACK`, `INTEGRATION`, `UPDATE`, `I18N`, `COMMAND`, with case-insensitive lookup `LogChannel.fromString(String)`.
- **CroppyLogger API**:
  - `log(LogLevel level, LogChannel channel, String message)` and throwable overload.
  - `log(LogLevel level, LogChannel channel, Supplier<String> messageSupplier)` for lazy formatting on `DEBUG`/`TRACE`.
  - Convenience methods (`trace`, `debug`, `info`, `warn`, `error`) and `scoped(String scope)` returning a child logger sharing the root filter state with a scope label (e.g., `Storage`, `Addon:coffee`).
  - `isEnabled(LogLevel level, LogChannel channel)` for multi-step guarded diagnostics.
- **Alternative Considered**: Keeping `java.util.logging.Logger` everywhere with a Paper `Handler` + naming conventions. Rejected because JUL has no typed channel filtering, no lazy `Supplier<String>` channel API, and Paper's JUL-to-Log4j bridge causes double-prefixing or fragile handler ordering.

### 2. Backwards-Compatible `AddonContext` JUL Bridge
Preserve `Logger AddonContext.getLogger()` unchanged and add `default CroppyLogger getCroppyLogger()` returning `JulCroppyLogger.wrap(getLogger(), LogChannel.ADDON)`.
- In `PaperAddonContext`, override `getCroppyLogger()` to return `croppyLogger.scoped("Addon:" + description.id())` targeting `LogChannel.ADDON`.
- Create an isolated JUL `Logger` (`"Croppy-Addon-" + id`) with `setUseParentHandlers(false)`, `setLevel(Level.ALL)`, and a bridge `Handler` mapping `SEVERE -> ERROR`, `WARNING -> WARN`, `INFO -> INFO`, `CONFIG/FINE -> DEBUG`, `FINER/FINEST -> TRACE` into the scoped `CroppyLogger`.
- **Rationale**: External addons and mock test implementations (such as `CroppyCoffeeAddonTest`) continue compiling and running untouched while gaining styled, level-filtered output.

### 3. Core, Loader & Integration Migration Strategy
- Migrate `CroppyCore`, `DefaultAddonManager`, `YamlAddonConfig`, `DefaultResourcePackCompiler`, `IntegrationManager`, `WorldGuardIntegration`, `WorldGuardFlags`, `PapiIntegration`, `VaultIntegration`, and all `Yaml*Loader` classes to `CroppyLogger` (defaulting to `LogChannel.CORE`, `LogChannel.LOADER`, `LogChannel.PACK`, `LogChannel.INTEGRATION`).
- Retain `Logger` compatibility overloads where existing tests instantiate services directly with `java.util.logging.Logger`, wrapping them via `JulCroppyLogger.wrap(...)`.
- Remove all static `Logger.getLogger(...)` fields from `DefaultResourcePackCompiler`, `YamlItemLoader`, and `YamlAddonConfig`.
- Demote per-entity loading messages (`Loaded crop species`, `Loaded fertilizer`, `Loaded X items from category`, `Loaded X messages for locale`) from `INFO` to `DEBUG` under `LogChannel.LOADER`.

### 4. Paper Console Renderer (`ComponentLogger` + `ConsoleCommandSender`)
Implement `PaperCroppyLogger` in `dev.darkblade.croppy.platform.paper.logging`:
- **Thread-Safe Runtime State**: `volatile LogLevel baseLevel`, `volatile LogStyle style` (`RICH` | `COMPACT`), `Set<LogChannel> debugChannels = ConcurrentHashMap.newKeySet()`, and atomic startup/reload warning & error counters.
- **Regular & Async Logs (`ComponentLogger`)**: All `info`, `warn`, `error`, `debug`, `trace`, and scoped logs format with MiniMessage/Adventure tags (sanitizing untrusted message strings via `Component.text(...)` so dynamic YAML errors or paths containing `<...>` cannot inject broken MiniMessage tags) and dispatch to `plugin.getComponentLogger()`. This guarantees thread safety from async tasks (updates, SQLite, resource pack build) and automatic `[Croppy]` prefixing.
- **Visual Multi-Line Blocks (`ConsoleCommandSender`)**: In `RICH` mode during synchronous main-thread lifecycle hooks (`onEnable`, `reloadPlugin`, `onDisable`), `ConsoleLifecycleReporter` writes the gradient banner (`<gradient:#70e000:#38b000>`) and aligned phase summary rows via `Bukkit.getConsoleSender().sendMessage(Component)` to avoid repeating `[Croppy]` on every banner line. In `COMPACT` mode, phase summaries emit single-line `[Croppy]` entries via `ComponentLogger`.

### 5. Phase-Streamed Lifecycle Reporting
- **`onLoad()`**: Load `PluginConfig` and initialize `PaperCroppyLogger` early without printing the startup banner, so early WorldGuard flag registration logs cleanly under `[Integration]`.
- **`onEnable()`**: Print header banner (`RICH`) or compact start line (`COMPACT`), reset warning/error counters, and run each logical phase (`Integrations`, `Storage`, `Localization`, `Definitions`, `Addons`, `Resource Pack`, `Runtime`) through `ConsoleLifecycleReporter`, printing each phase line immediately upon completion with elapsed milliseconds (`[Xms]`). End with a status footer reporting total elapsed time and any warning/error count.
- **`reloadPlugin()`**: Re-read `config.yml`, call `paperLogger.applyConfig(pluginConfig)` (resetting runtime debug channel overrides back to `config.yml`), execute reload phases, and emit a concise reload summary block (omitting the full ASCII banner).
- **`onDisable()`**: Flush active crops/stations, cancel tasks/listeners, shut down core/integrations, and emit a compact shutdown summary with flushed counts and duration.

### 6. Configuration & `/croppy debug` Command
- **`config.yml` & `PluginConfig`**: Add `logging.level` (`INFO`), `logging.style` (`RICH`), and `logging.debug-channels` (`[]`). Invalid values log a warning and fall back to defaults.
- **`CroppyCommands`**: Add `@Command("croppy debug status")`, `toggle <channel>`, `enable <channel>`, `disable <channel>`, `all`, `off` guarded by `@Permission("croppy.admin")` with Cloud channel autocompletion and localized MiniMessage feedback in `en-US.yml` and `es-ES.yml`.

## Risks / Trade-offs

- **Risk**: JUL `Handler` bridge on addon loggers emits duplicate messages via root JUL logger → **Mitigation**: Set `useParentHandlers = false` and `Level.ALL` on bridged addon loggers so `PaperCroppyLogger` is the sole filter and sink.
- **Risk**: Untrusted strings (file paths, exception messages, YAML values) containing `<` or `>` break MiniMessage deserialization in console logs → **Mitigation**: Construct dynamic message payloads using `Component.text(message)` appended to styled prefix components rather than passing raw log text through `MiniMessage.deserialize(...)`.
- **Risk**: `ConsoleCommandSender.sendMessage()` called off the main thread → **Mitigation**: Restrict `ConsoleCommandSender` strictly to main-thread `ConsoleLifecycleReporter` calls during `onEnable`/`reloadPlugin`/`onDisable`; route all runtime/async messages through thread-safe `ComponentLogger`.
- **Risk**: `LocaleResourcesIntegrityTest` fails if `es-ES.yml` is out of sync with `en-US.yml` → **Mitigation**: Add all `command.debug.*` keys to both `en-US.yml` and `es-ES.yml` together and verify with Gradle test suite.
