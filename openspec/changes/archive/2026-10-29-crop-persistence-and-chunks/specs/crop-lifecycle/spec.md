# Spec Delta: crop-lifecycle

## ADDED Requirements

### Requirement: Chunk-Bounded In-Memory Crop Lifecycle
The system SHALL bind active in-memory crop instances to chunk lifecycle events, registering crops when their containing chunk loads and saving and evicting them when their chunk unloads.

#### Scenario: Chunk load registers crops into active memory
- **WHEN** a chunk containing saved crops is loaded by the world
- **THEN** the system SHALL load the crops from storage and register them into the active tracker

#### Scenario: Chunk unload evicts crops from active memory
- **WHEN** a chunk containing active crops is unloaded by the world
- **THEN** the system SHALL save the crop states to storage and remove them from the active tracker
