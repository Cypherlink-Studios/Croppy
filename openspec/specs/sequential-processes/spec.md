# sequential-processes Specification

## Purpose
Provides a modular finite state machine engine for executing multi-phase artisan processing and culinary crafting recipes with dynamic quality resolution.

## Requirements

### Requirement: Multi-Phase State Machine Execution
The process engine SHALL manage the state transitions (`IDLE`, `RUNNING`, `AWAITING_INPUT`, `COMPLETED`, `FAILED`) of multi-phase recipes tracked on stations, resolving matching recipes and binding recipe context variables.

#### Scenario: Initiate recipe process
- **WHEN** valid inputs matching a registered recipe and start conditions are satisfied at an idle station
- **THEN** a `ProcessInstance` transitions to `RUNNING` at phase index 0 for the recipe's bound process definition and consumes required starting ingredients

#### Scenario: Phase sequential transition
- **WHEN** current phase criteria are satisfied
- **THEN** the process increments its active phase index, resets phase elapsed ticks, and executes phase entry actions

#### Scenario: Complete recipe process with outputs
- **WHEN** the final phase of a process instance completes
- **THEN** the process transitions to `COMPLETED` and produces the configured recipe output item and amount into station storage

### Requirement: Phase Types and Advancement Triggers
The engine SHALL support diverse phase advancement types including `TIMED`, `MANUAL_INTERACTION`, and `ENVIRONMENTAL`, supporting linear duration ticking and thermal condition hold.

#### Scenario: Timed phase completion
- **WHEN** a `TIMED` phase reaches its target `duration_ticks` while conditions remain valid
- **THEN** the phase completes and transitions to the subsequent phase

#### Scenario: Timed phase condition pause
- **WHEN** a `TIMED` phase condition evaluates to false and condition pause is active
- **THEN** the phase elapsed duration is frozen and failure is deferred until the condition recovers or timeout ticks elapse

#### Scenario: Manual interaction advance
- **WHEN** a process is in an `AWAITING_INPUT` phase and a player interacts with the required item
- **THEN** the interaction is validated, item consumed if specified, and the process advances

#### Scenario: Environmental threshold advance
- **WHEN** an `ENVIRONMENTAL` phase's target condition expression evaluates to true
- **THEN** the phase transitions without requiring player action

### Requirement: Failure Policies and Timeouts
The process engine SHALL enforce timeouts and condition integrity, applying configured failure policies (`CANCEL`, `SPOIL`, `BYPRODUCT`) when violations occur.

#### Scenario: Interactive phase timeout spoil
- **WHEN** an interactive phase exceeds its `timeout_ticks` without player intervention
- **THEN** the process transitions to `FAILED` and converts contents into a spoiled byproduct according to policy

#### Scenario: Environmental condition failure
- **WHEN** a required condition fails continuously beyond allowable grace ticks
- **THEN** the process pauses or terminates depending on definition settings

### Requirement: Dynamic Output and Quality Resolution
The process engine SHALL evaluate completed processes to produce output items with quality tiers determined by ingredient inputs and execution accuracy.

#### Scenario: High quality ingredients produce high quality output
- **WHEN** all input ingredients used in a process have Gold quality rating
- **THEN** the output resolver assigns Gold or higher quality tier to the resulting product

#### Scenario: Successful execution bonus
- **WHEN** all timed and interactive phases are completed within optimal timing windows
- **THEN** the quality score receives a bonus modifier boosting the final item grade

### Requirement: Process Definition YAML Registry and Packaging
The system SHALL discover, parse, and register process definitions from `processes/*.yml` files into the `ProcessRegistry`, and unpack default bundled process configurations upon startup.

#### Scenario: Load process definition from YAML
- **WHEN** the plugin initializes or reloads with valid process configuration files in `processes/`
- **THEN** each file is parsed into a `ProcessDefinition` with structured phases and registered in `ProcessRegistry`

#### Scenario: Unpack bundled default process files
- **WHEN** the server starts with missing default process files
- **THEN** default files for `tomato_soup_cook.yml`, `vegetable_stew_cook.yml`, and `fried_egg_cook.yml` are unpacked into the server's `processes/` directory

### Requirement: Configurable Phase Display and Placeholder Resolution
The system SHALL render customizable phase display status holograms using MiniMessage templates enriched with direct and inverted (`_left`) progress, time, and progress bar placeholders.

#### Scenario: Render formatted phase status with progress bar
- **WHEN** an active process is ticked during a `TIMED` phase
- **THEN** placeholders including `{progress_percent}`, `{progress_percent_left}`, `{progress_seconds_left}`, and `{progress_bar}` are resolved and displayed on the station hologram

#### Scenario: Render interaction prompt with remaining timeout
- **WHEN** an active process is in `AWAITING_INPUT` state
- **THEN** configured prompt text and remaining timeout placeholders are resolved and displayed on the station hologram

### Requirement: Phase Multi-Sensory Feedback
The system SHALL emit phase-specific ambient particles and periodic sound effects during active process execution.

#### Scenario: Active phase particle emission
- **WHEN** an active process phase defines particle effects and executes a simulation tick
- **THEN** the configured particle type, count, and offset are spawned around the station block

#### Scenario: Active phase periodic sound trigger
- **WHEN** an active process phase reaches its configured sound interval ticks
- **THEN** the configured sound is played at the station location with defined volume and pitch
