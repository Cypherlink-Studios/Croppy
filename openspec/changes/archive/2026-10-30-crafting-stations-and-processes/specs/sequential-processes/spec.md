# Spec Delta: Sequential Processes

## Purpose

Provides a modular finite state machine engine for executing multi-phase artisan processing and culinary crafting recipes with dynamic quality resolution.

## ADDED Requirements

### Requirement: Multi-Phase State Machine Execution
The process engine SHALL manage the state transitions (`IDLE`, `RUNNING`, `AWAITING_INPUT`, `COMPLETED`, `FAILED`) of multi-phase recipes tracked on stations.

#### Scenario: Initiate recipe process
- **WHEN** valid inputs and start conditions are matched at an idle station
- **THEN** a `ProcessInstance` transitions to `RUNNING` at phase index 0 and consumes required starting ingredients

#### Scenario: Phase sequential transition
- **WHEN** current phase criteria are satisfied
- **THEN** the process increments its active phase index, resets phase elapsed ticks, and executes phase entry actions

### Requirement: Phase Types and Advancement Triggers
The engine SHALL support diverse phase advancement types including `TIMED`, `MANUAL_INTERACTION`, and `ENVIRONMENTAL`.

#### Scenario: Timed phase completion
- **WHEN** a `TIMED` phase reaches its target `duration_ticks` while conditions remain valid
- **THEN** the phase completes and transitions to the subsequent phase

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
