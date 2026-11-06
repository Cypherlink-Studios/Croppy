# interaction-engine Specification

## Purpose
Enables declarative, data-driven interaction definitions that route player clicks, sneak status, held tools, and condition checks to customizable station actions without hardcoded listeners.

## Requirements

### Requirement: Declarative Interaction Definitions
The system SHALL load immutable `InteractionDefinition` configurations from YAML files mapping player interaction triggers (including right-click, sneak-right-click, left-click, and sneak-left-click) and matching filters to sequenceable gameplay actions targeting stations or crop species.

#### Scenario: Load interaction definition from YAML
- **WHEN** an interaction YAML file defining triggers for `croppy:cooking_pot` is loaded
- **THEN** it is registered in the interaction registry and indexed by target station and trigger mode

#### Scenario: Load crop interaction definition
- **WHEN** an interaction YAML file defining triggers for a crop species is loaded
- **THEN** it is registered in the interaction registry and indexed by target crop species and trigger mode

#### Scenario: Load sneak left-click trigger
- **WHEN** an interaction definition specifies trigger as SNEAK_LEFT_CLICK
- **THEN** the definition SHALL be registered to evaluate on player shift-punch interactions

### Requirement: Interaction Routing and Dispatch
The system SHALL evaluate registered interaction definitions when a player interacts with a station or crop, evaluating context variables including player state, tool in hand, and entity-specific state, selecting the highest-priority matching definition, and executing its configured action pipeline.

#### Scenario: Route sneak right-click to GUI
- **WHEN** a player sneak-right-clicks a station matching conditions for a GUI interaction definition
- **THEN** the virtual container GUI opens for the player and cancels physical extraction

#### Scenario: Route matching tool interaction
- **WHEN** a player right-clicks a station holding a matching custom tool or item
- **THEN** the matching interaction actions execute and hand item consumption occurs according to the definition

#### Scenario: Route based on storage state conditions
- **WHEN** an interaction definition defines storage conditions (such as `storage.has_inputs == true` or `storage.is_empty == true`)
- **THEN** the interaction router evaluates the station's storage occupancy against the condition expressions to determine a match

#### Scenario: Route matching crop interaction
- **WHEN** a player interacts with a crop matching conditions for a crop interaction definition
- **THEN** the interaction router evaluates context variables (including `crop.stage_id` and `crop.is_mature`) and executes the matching pipeline

### Requirement: Extensible Action Execution
The system SHALL support extensible action types within interaction pipelines including opening GUIs, displaying action bar notices, inserting or extracting inputs, extracting outputs, advancing process ticks, harvesting crops, dismantling crops, playing sounds, and emitting particles, resolving custom registered items and vanilla materials when spawning item drops.

#### Scenario: Advance process progress on tool action
- **WHEN** an interaction definition executes an `ADVANCE_PROGRESS` action with a valid tool
- **THEN** the station active process progress advances by the specified tick count and feedback effects trigger

#### Scenario: Extract inputs action execution
- **WHEN** an interaction definition executes an `EXTRACT_INPUTS` action
- **THEN** all stored input ingredients are extracted from the station and placed into the player's inventory

#### Scenario: Harvest crop action execution
- **WHEN** an interaction definition executes a `HARVEST_CROP` action on a mature crop
- **THEN** harvest yields SHALL be dropped resolving custom items from `ItemRegistry` and vanilla materials and the crop SHALL follow its configured regrowth policy

#### Scenario: Dismantle crop action execution
- **WHEN** an interaction definition executes a `DISMANTLE_CROP` action on a crop
- **THEN** the crop instance and all its multi-block occupancy positions SHALL be completely removed and dismantling drops SHALL be spawned resolving custom items and vanilla materials
