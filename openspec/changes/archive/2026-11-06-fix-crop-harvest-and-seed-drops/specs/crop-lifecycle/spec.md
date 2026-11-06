# Spec Delta

## MODIFIED Requirements

### Requirement: Crop Harvest and Destruction Lifecycle
The system SHALL handle crop destruction and harvesting, dropping configured yields for mature crops or seeds for immature crops resolving both custom items from `ItemRegistry` and native vanilla materials, deleting stored records using canonical root positions, setting physical base blocks to air upon virtual dismantling, and either reverting the developmental stage for crops with regrowth or unregistering monocarpic instances.

#### Scenario: Mature crop harvest
- **WHEN** a player breaks a tracked crop that is at its mature stage
- **THEN** the system SHALL drop items defined in the species harvest yield resolving custom items and vanilla materials
- **THEN** the crop instance SHALL be removed from the active tracker and deleted from persistence using its root position

#### Scenario: Immature crop breakage
- **WHEN** a player or environmental physics breaks an immature crop
- **THEN** the system SHALL drop the species seed item resolving custom items and vanilla materials
- **THEN** the crop instance SHALL be removed from the active tracker and deleted from persistence using its root position

#### Scenario: Mature crop harvest with regrowth
- **WHEN** a player harvests a mature crop configured with a valid regrowth target stage
- **THEN** the system SHALL drop items defined in the species harvest yield resolving custom items and vanilla materials
- **THEN** the crop instance SHALL remain tracked, its stage index SHALL revert to the target stage, and its progress SHALL reset to 0.0

#### Scenario: Virtual dismantling clears physical base block
- **WHEN** a player dismantles a virtual 3D crop via packet interaction
- **THEN** the system SHALL set the physical base block in the world to air and remove all virtual display entities
