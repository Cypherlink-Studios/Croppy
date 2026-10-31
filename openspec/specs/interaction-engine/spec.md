# interaction-engine Specification

## Purpose
Enables declarative, data-driven interaction definitions that route player clicks, sneak status, held tools, and condition checks to customizable station actions without hardcoded listeners.

## Requirements

### Requirement: Declarative Interaction Definitions
The system SHALL load immutable `InteractionDefinition` configurations from YAML files in `content/interactions/` mapping player interaction triggers and matching filters to sequenceable gameplay actions.

#### Scenario: Load interaction definition from YAML
- **WHEN** an interaction YAML file defining triggers for `croppy:cooking_pot` is loaded
- **THEN** it is registered in the interaction registry and indexed by target station and trigger mode

### Requirement: Interaction Routing and Dispatch
The system SHALL evaluate registered interaction definitions when a player right-clicks or sneak-right-clicks a station, selecting the highest-priority matching definition and executing its configured action pipeline.

#### Scenario: Route sneak right-click to GUI
- **WHEN** a player sneak-right-clicks a hybrid station configured with a sneak GUI interaction
- **THEN** the virtual container GUI opens for the player and cancels physical extraction

#### Scenario: Route matching tool interaction
- **WHEN** a player right-clicks a station holding a matching custom tool or item
- **THEN** the matching interaction actions execute and hand item consumption occurs according to the definition

### Requirement: Extensible Action Execution
The system SHALL support extensible action types within interaction pipelines including opening GUIs, displaying action bar notices, inserting or extracting items, advancing process ticks, playing sounds, and emitting particles.

#### Scenario: Advance process progress on tool action
- **WHEN** an interaction definition executes an `ADVANCE_PROGRESS` action with a valid tool
- **THEN** the station active process progress advances by the specified tick count and feedback effects trigger
