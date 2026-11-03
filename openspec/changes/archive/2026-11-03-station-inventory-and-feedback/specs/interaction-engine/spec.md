# Spec Delta: interaction-engine

## MODIFIED Requirements

### Requirement: Interaction Routing and Dispatch
The system SHALL evaluate registered interaction definitions when a player right-clicks or sneak-right-clicks a station, evaluating context variables including player state, temperature, process status, and storage occupancy, selecting the highest-priority matching definition, and executing its configured action pipeline.

#### Scenario: Route sneak right-click to GUI
- **WHEN** a player sneak-right-clicks a station matching conditions for a GUI interaction definition
- **THEN** the virtual container GUI opens for the player and cancels physical extraction

#### Scenario: Route matching tool interaction
- **WHEN** a player right-clicks a station holding a matching custom tool or item
- **THEN** the matching interaction actions execute and hand item consumption occurs according to the definition

#### Scenario: Route based on storage state conditions
- **WHEN** an interaction definition defines storage conditions (such as `storage.has_inputs == true` or `storage.is_empty == true`)
- **THEN** the interaction router evaluates the station's storage occupancy against the condition expressions to determine a match

### Requirement: Extensible Action Execution
The system SHALL support extensible action types within interaction pipelines including opening GUIs, displaying action bar notices, inserting or extracting inputs, extracting outputs, advancing process ticks, playing sounds, and emitting particles.

#### Scenario: Advance process progress on tool action
- **WHEN** an interaction definition executes an `ADVANCE_PROGRESS` action with a valid tool
- **THEN** the station active process progress advances by the specified tick count and feedback effects trigger

#### Scenario: Extract inputs action execution
- **WHEN** an interaction definition executes an `EXTRACT_INPUTS` action
- **THEN** all stored input ingredients are extracted from the station and placed into the player's inventory
