# Spec Delta

## ADDED Requirements

### Requirement: Hot Base Model and Hitbox State Metadata Synchronization
The system SHALL dynamically synchronize workstation base model items, transformations, interaction hitbox dimensions, and ambient sensory effects via packet metadata when a station transitions between states without respawning or despawning virtual entities.

#### Scenario: Update base model item display metadata on state change
- **WHEN** a workstation instance transitions to a new active state defining an item_model or custom_model_data override
- **THEN** entity metadata packets updating the Item Display item and transform are dispatched to all tracking players without destroying or respawning the virtual display entity

#### Scenario: Update interaction hitbox dimensions on state change
- **WHEN** a workstation transitions to an active state specifying custom hitbox width and height
- **THEN** entity metadata packets updating the interaction entity dimensions are dispatched to tracking players

#### Scenario: Emit ambient state sensory effects during station ticking
- **WHEN** an active station state defines ambient particle or sound effects and the workstation ticks
- **THEN** configured particles are spawned and periodic sound loops are played at the workstation position
