# virtual-3d-block-displays Specification

## Purpose

Provides client-side packet-based virtual 3D display rendering (Item Display) coupled with virtual Interaction hitboxes for crops and functional blocks via PacketEvents, featuring player placement orientation and inbound click packet routing without server entity persistence.

## Requirements

### Requirement: Virtual 3D Model and Interaction Entity Pairing
The system SHALL project a pair of client-side virtual entities for each 3D crop and functional block: an ItemDisplay rendering the 3D model and an Interaction entity intercepting player raycasts and clicks, without creating or persisting entities in the world chunk NBT.

#### Scenario: Spawning virtual display pair
- **WHEN** a player enters tracking range of a crop or block configured with a 3D model
- **THEN** the system SHALL dispatch PacketEvents spawn and metadata packets for an ItemDisplay entity and an Interaction entity positioned at the block center

#### Scenario: Despawning virtual display pair
- **WHEN** a player leaves tracking range or the crop or block is removed
- **THEN** the system SHALL dispatch PacketEvents destroy packets removing both the ItemDisplay and Interaction entities from the client

### Requirement: Dynamic Growth Stage Hitbox Scaling
The system SHALL scale the virtual Interaction entity's width and height dynamically according to the active developmental stage of the crop.

#### Scenario: Hitbox adjusts on stage advancement
- **WHEN** a crop advances from a seedling stage to a mature stage
- **THEN** the system SHALL dispatch metadata packets updating the Interaction entity's height and width to match the new stage's configured hitbox dimensions

### Requirement: Player Placement Orientation and Rotation Modes
The system SHALL capture the player's yaw orientation upon planting or placing a 3D block and apply the corresponding Y-axis rotation quaternion to the virtual ItemDisplay according to the configured rotation mode.

#### Scenario: Cardinal four-direction snapping
- **WHEN** a block configured with rotation mode CARDINAL_4 is placed
- **THEN** the system SHALL snap the player's yaw to the nearest 90-degree cardinal angle and orient the ItemDisplay accordingly

#### Scenario: Continuous player yaw orientation
- **WHEN** a crop configured with rotation mode FREE_YAW is planted
- **THEN** the system SHALL preserve the exact player yaw and orient the ItemDisplay quaternion to match

#### Scenario: Random jitter rotation
- **WHEN** a crop configured with rotation mode RANDOM_JITTER is planted
- **THEN** the system SHALL apply a randomized angle offset within plus or minus 15 degrees to the base placement yaw

### Requirement: Inbound Packet Click Routing
The system SHALL intercept client entity interaction packets targeting virtual Interaction entities and route them to corresponding crop and block handlers on the server main thread.

#### Scenario: Left-click attack packet routes to break or punch
- **WHEN** a client sends an interact entity packet with action ATTACK targeting a registered virtual Interaction entity
- **THEN** the system SHALL dispatch the attack interaction to the main thread to handle crop or block breaking

#### Scenario: Right-click interact packet routes to tool and harvest actions
- **WHEN** a client sends an interact entity packet with action INTERACT targeting a registered virtual Interaction entity
- **THEN** the system SHALL dispatch the interaction to the main thread to evaluate tool usage, watering, or crop harvesting
