# Spec Delta: Plugin Lifecycle

## Purpose

Coordinates the lifecycle stages of Croppy's decoupled core services and provides a clean bridge to the Paper 1.21.8 runtime environment.

## ADDED Requirements

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
The system SHALL provide a Paper platform adapter that binds JavaPlugin enable and disable lifecycle hooks to the core orchestrator.

#### Scenario: Paper plugin enable
- **WHEN** Paper enables the Croppy plugin
- **THEN** the platform adapter SHALL initialize the core orchestrator and log successful enablement

#### Scenario: Paper plugin disable
- **WHEN** Paper disables the Croppy plugin
- **THEN** the platform adapter SHALL invoke core shutdown and cleanly release platform resources
