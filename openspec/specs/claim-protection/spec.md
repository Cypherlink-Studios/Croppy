# claim-protection Specification

## Purpose

Provides spatial permission validation and land claim protection across agricultural and culinary interactions, shielding crops and workstations from unauthorized player access.

## Requirements

### Requirement: Spatial Interaction Protection Check
The system SHALL evaluate land claim and region protection permissions before allowing any player to plant, harvest, water, fertilize, or interact with physical crops and cooking stations.

#### Scenario: Authorized player harvests crop
- **WHEN** a player with permission in the region right-clicks a mature crop
- **THEN** the interaction is permitted and crop harvest proceeds normally

#### Scenario: Unauthorized player attempts to harvest crop
- **WHEN** a player lacking permission in the region right-clicks a mature crop
- **THEN** the interaction is cancelled and no crop state is altered

### Requirement: Virtual Entity Interaction Protection
The system SHALL intercept client entity interaction packets targeting virtual 3D crop displays and cooking station hitboxes and enforce protection validation prior to executing harvest or station actions.

#### Scenario: Unauthorized virtual entity click
- **WHEN** a player lacking region permission clicks on a virtual 3D crop display or station hitbox
- **THEN** the system denies the action and prevents state manipulation

### Requirement: WorldGuard Region and Custom Flag Integration
The system SHALL evaluate WorldGuard region flags at target coordinates, checking custom Croppy state flags (`croppy-plant`, `croppy-harvest`, `croppy-water`, `croppy-fertilize`, `croppy-cook`) when defined, and falling back to standard WorldGuard build and interact flags when custom flags are unset.

#### Scenario: Region defines custom croppy flag
- **WHEN** a player attempts an action in a region where the corresponding `croppy-*` flag is explicitly set to ALLOW
- **THEN** the system permits the interaction regardless of general build restrictions

#### Scenario: Region uses fallback flags
- **WHEN** a player attempts an action in a region where custom Croppy flags are unset
- **THEN** the system evaluates standard WorldGuard build and interact flags to determine access

### Requirement: Fallback No-Op Protection
The system SHALL provide a transparent fallback protection provider when WorldGuard or claim integration is disabled or absent, permitting all interactions without runtime errors.

#### Scenario: WorldGuard is not present
- **WHEN** a player interacts with a crop or cooking station and WorldGuard is not installed
- **THEN** the system permits the interaction and logs no errors
