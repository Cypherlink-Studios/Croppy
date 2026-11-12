# Spec Delta

## Purpose

Provides a decoupled, channel-aware structured logging abstraction across core and platform modules, pairing lazy diagnostic filtering with configurable `RICH` and `COMPACT` console presentation.

## ADDED Requirements

### Requirement: Domain Logging Port Isolation
The system SHALL expose a pure Java domain logging abstraction supporting severity levels (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`), diagnostic channels, lazy message suppliers, and scoped child loggers without depending on platform or formatting libraries.

#### Scenario: Creating a scoped child logger
- **WHEN** a subsystem requests a child logger with a scope label such as `Storage` or `Addon:coffee`
- **THEN** emitted log records include that scope context while inheriting the parent logger's filtering rules

#### Scenario: Headless unit testing without platform runtime
- **WHEN** core domain services or YAML loaders execute in standalone Java unit tests
- **THEN** they operate using an in-memory or standard domain logger implementation without requiring Paper or Adventure classes

### Requirement: Channel-Based Filtering and Severity Control
The system SHALL evaluate whether each log entry is emitted based on the active base severity level and the set of explicitly enabled diagnostic channels, directing all `DEBUG` and `TRACE` output exclusively to the server console.

#### Scenario: Filtering suppressed debug channels
- **WHEN** the active log level is `INFO` and the `GROWTH` channel is not enabled
- **THEN** `DEBUG` and `TRACE` messages emitted on `GROWTH` are suppressed from console output

#### Scenario: Channel-specific debug override
- **WHEN** the active log level is `INFO` and the `PACKET` channel is enabled
- **THEN** `DEBUG` messages on `PACKET` appear in the console while `DEBUG` messages on other unenabled channels remain suppressed

### Requirement: Zero-Allocation Lazy Debug Evaluation
The system SHALL support lazy `Supplier<String>` message evaluation on `DEBUG` and `TRACE` operations so suppressed diagnostic logs do not construct formatted strings on hot execution paths.

#### Scenario: Skipping string evaluation for disabled channels
- **WHEN** a growth ticker or packet listener logs a `TRACE` or `DEBUG` message via a supplier while that channel is disabled
- **THEN** the supplier lambda is never invoked

### Requirement: Loader Granularity and Orphan Logger Elimination
The system SHALL route per-entity YAML loading messages to `DEBUG` under the `LOADER` channel, surface parsing or validation errors immediately at `WARN` or `ERROR`, and eliminate standalone static loggers that bypass injected plugin context.

#### Scenario: Quiet startup during healthy content loading
- **WHEN** YAML definitions for items, species, fertilizers, and locales load without errors at `INFO` level
- **THEN** individual entry names are omitted from `INFO` output and surfaced only when `LOADER` debug logging or `DEBUG` level is enabled

#### Scenario: Immediate surfacing of malformed YAML definitions
- **WHEN** a YAML definition file fails validation or contains syntax errors during loading
- **THEN** the error is emitted immediately at `WARN` or `ERROR` with source context without aborting healthy definitions

### Requirement: Platform Console Rendering and Visual Styles
The system SHALL render console output using Paper's Adventure logging pipeline, supporting `RICH` mode with a styled multi-line banner and phase-streamed summaries, and `COMPACT` mode with single-line `[Croppy]` prefixed entries, while ensuring plain-text log files contain no raw ANSI codes.

#### Scenario: Rendering RICH visual style
- **WHEN** `logging.style` is configured as `RICH`
- **THEN** startup prints a gradient header banner followed by aligned phase summary rows with millisecond durations and status indicators

#### Scenario: Rendering COMPACT visual style
- **WHEN** `logging.style` is configured as `COMPACT`
- **THEN** startup omits multi-line ASCII art and emits concise `[Croppy]` prefixed phase summary lines

#### Scenario: Thread-safe asynchronous log output
- **WHEN** background update checks, resource pack compilation, or async storage tasks emit logs
- **THEN** the platform logger dispatches styled records safely without requiring main-thread execution

### Requirement: Logging Configuration Schema
The system SHALL read `logging.level` (default `INFO`), `logging.style` (default `RICH`), and `logging.debug-channels` (default empty list) from `config.yml`, falling back safely to defaults when keys are absent or invalid.

#### Scenario: Loading default logging configuration
- **WHEN** `config.yml` omits the `logging` section
- **THEN** the configuration defaults to level `INFO`, style `RICH`, and no active debug channel overrides

#### Scenario: Parsing configured debug channels case-insensitively
- **WHEN** `config.yml` specifies `logging.debug-channels` with mixed-case channel names such as `growth` or `Station`
- **THEN** the corresponding `LogChannel` entries are activated without error

### Requirement: Silent Exception Diagnostic Instrumentation
The system SHALL replace silent exception catches in runtime tickers, virtual display renderers, target raytracing, and interaction listeners with `TRACE` or `DEBUG` logging under their subsystem channel.

#### Scenario: Tracing invalid particle or sound names in station effects
- **WHEN** a station phase or rule triggers a malformed particle or sound effect while the `STATION` channel is enabled at `TRACE`
- **THEN** the platform logger emits the diagnostic reason and context to console without interrupting the station tick
