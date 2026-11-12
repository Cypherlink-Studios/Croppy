# Spec Delta

## MODIFIED Requirements

### Requirement: Base Block Client Masking
The system SHALL mask the physical station base block with a client-side packet block change for viewing players when `hide_base_block` is enabled in the station visual configuration, re-asserting the mask across chunk transmissions and player connections.

#### Scenario: Conceal physical base block when player enters viewing range
- **WHEN** a player enters the viewing range of a station configured with `hide_base_block: true`
- **THEN** a block change packet representing an invisible barrier or air is dispatched to the player's connection while the server world retains the registered base block

#### Scenario: Conceal physical base block when player receives chunk packet
- **WHEN** a player receives a chunk packet (`PlayerChunkLoadEvent`) containing one or more registered stations configured with `hide_base_block: true`
- **THEN** client block change packets masking each station base block as an invisible barrier are dispatched immediately to the player's connection, preventing the chunk's physical block from rendering

#### Scenario: Project station displays immediately upon player login
- **WHEN** a player joins the server and is within viewing range of a registered crafting station
- **THEN** station virtual display entities and base block masking are evaluated and projected immediately without waiting for the recurring ticker cycle

#### Scenario: Restore real block appearance when station is broken or unregistered
- **WHEN** a station with base block masking is broken or removed from the world
- **THEN** virtual display entities are destroyed and block state updates reflect the true world state for tracking players
