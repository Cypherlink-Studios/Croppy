# Spec Delta

## MODIFIED Requirements

### Requirement: Inbound Packet Click Routing
The system SHALL intercept client entity interaction packets targeting virtual Interaction entities and route them to corresponding crop and block handlers on the server main thread, including specialized inspection tool handling.

#### Scenario: Left-click attack packet routes to break or punch
- **WHEN** a client sends an interact entity packet with action ATTACK targeting a registered virtual Interaction entity
- **THEN** the system SHALL dispatch the attack interaction to the main thread to handle crop or block breaking

#### Scenario: Right-click interact packet routes to tool and harvest actions
- **WHEN** a client sends an interact entity packet with action INTERACT targeting a registered virtual Interaction entity
- **THEN** the system SHALL dispatch the interaction to the main thread to evaluate tool usage, watering, or crop harvesting

#### Scenario: Right-click interact packet with inspection tool routes to inspection
- **WHEN** a client sends an interact entity packet with action INTERACT targeting a registered virtual Interaction entity while holding `croppy:inspection_tool`
- **THEN** the system SHALL dispatch the interaction to the crop inspection service to display the crop status card without harvesting the crop

## ADDED Requirements

### Requirement: Virtual Entity Line-of-Sight Raytracing
The system SHALL provide spatial line-of-sight raytracing against active virtual Interaction entity bounding boxes derived from developmental stage hitbox dimensions, enabling command resolution for 3D crops with minimal or passable base blocks.

#### Scenario: Player ray intersects virtual interaction entity
- **WHEN** a player executes a targeted command pointing at a virtual Interaction entity within reach distance without intervening solid block occlusion
- **THEN** the system SHALL resolve the underlying crop instance corresponding to that virtual display

#### Scenario: Player ray occluded by solid obstacle
- **WHEN** a solid opaque block intervenes between the player's eye location and the virtual Interaction entity along the ray vector
- **THEN** the system SHALL reject the virtual entity hit and resolve only unoccluded targets
