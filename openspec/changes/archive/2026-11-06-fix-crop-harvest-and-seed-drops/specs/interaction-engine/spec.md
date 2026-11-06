# Spec Delta

## MODIFIED Requirements

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
