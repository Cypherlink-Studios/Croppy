# plugin-lifecycle Specification

## Purpose
Coordinates the lifecycle stages of Croppy's decoupled core services and provides a clean bridge to the Paper 1.21.8 runtime environment.

## Requirements

### Requirement: Core Service Lifecycle Orchestration
The system SHALL provide an independent lifecycle coordinator managing the initialization, service binding, and graceful shutdown of core domain components.

#### Scenario: Core startup sequence
- **WHEN** the core lifecycle orchestrator executes startup
- **THEN** internal registries and core services SHALL transition to an active state
- **THEN** core services SHALL become queryable from the service container

#### Scenario: Core shutdown sequence
- **WHEN** the core lifecycle orchestrator executes shutdown
- **THEN** active services SHALL release resources and transition to a stopped state
- **THEN** registries SHALL prevent further mutative operations

### Requirement: Platform Adapter Initialization
The system SHALL provide a Paper platform adapter that initializes early logging during plugin load and binds JavaPlugin enable and disable lifecycle hooks to the core orchestrator and structured console reporting.

#### Scenario: Paper plugin early load
- **WHEN** Paper invokes `onLoad` on the Croppy plugin
- **THEN** the platform adapter SHALL load logging configuration and initialize the structured logger without printing the startup banner so early integration registration (such as WorldGuard flags) uses unified formatting

#### Scenario: Paper plugin enable
- **WHEN** Paper enables the Croppy plugin
- **THEN** the platform adapter SHALL initialize the core orchestrator, stream phase-grouped startup summaries with timing metrics, and log successful enablement

#### Scenario: Paper plugin disable
- **WHEN** Paper disables the Croppy plugin
- **THEN** the platform adapter SHALL invoke core shutdown, log the consolidated flush summary for crops and stations, and cleanly release platform resources

### Requirement: Phase-Streamed Startup and Reload Reporting
The system SHALL stream lifecycle phase summaries immediately as each initialization stage completes and track accumulated startup warnings and errors in the enable completion footer.

#### Scenario: Streaming phase results on startup failure
- **WHEN** an initialization phase fails mid-startup during `onEnable`
- **THEN** all previously completed phases have already been printed in console and the failing phase error is logged immediately before disabling the plugin

#### Scenario: Reporting warnings in startup footer
- **WHEN** one or more non-fatal warnings occur during startup phases (such as a malformed YAML definition or HTTP port bind conflict)
- **THEN** the completion footer reports the total elapsed time alongside the warning or error count

#### Scenario: Compact summary on plugin reload
- **WHEN** the plugin reloads configuration and registries at runtime
- **THEN** the platform adapter emits a concise reload summary showing updated registry counts and elapsed duration without repeating the full startup ASCII banner
