# Spec Delta

## ADDED Requirements

### Requirement: Normalized Model UV Bounds
The companion resource pack SHALL declare all element face UV coordinates in custom block and item models within the normalized bounds of 0.0 to 16.0 on both axes, preventing texture atlas oversampling and adjacent sprite bleeding across multi-block heights and non-square textures.

#### Scenario: Validating model face UV coordinates
- **WHEN** a custom block or item model JSON in `assets/croppy/models/` is evaluated
- **THEN** all UV coordinate values across every element face are greater than or equal to 0.0 and less than or equal to 16.0

#### Scenario: Rendering multi-block crop stages
- **WHEN** a 2-block crop model with a 16x32 texture renders in the world
- **THEN** the model samples the full vertical sprite within the normalized 0.0 to 16.0 UV range without bleeding into neighboring atlas textures
