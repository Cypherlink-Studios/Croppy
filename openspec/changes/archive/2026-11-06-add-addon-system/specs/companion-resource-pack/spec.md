# Spec Delta

## MODIFIED Requirements

### Requirement: Namespaced Asset Hierarchy and Path Compliance
All resource pack assets SHALL be contained under `assets/<namespace>/`, where `<namespace>` is `croppy` or any registered addon namespace. Every directory and filename within the assets tree MUST strictly adhere to the lowercase regex `^[a-z0-9_.-]+$`.

#### Scenario: Auditing asset paths
- **WHEN** the asset directory tree is traversed across core and addon namespaces
- **THEN** all path segments contain only lowercase alphanumeric characters, dots, underscores, and dashes

#### Scenario: Supporting multi-namespace asset trees
- **WHEN** an addon declares namespace "winery" and provides assets in `assets/winery/`
- **THEN** the system validates paths under `assets/winery/` against regex `^[a-z0-9_.-]+$` without requiring assets to live under `assets/croppy/`

## ADDED Requirements

### Requirement: Automated Addon Asset Merging
The companion resource pack compiler SHALL discover `assets/` subdirectories inside enabled addons and merge their models, textures, and sounds into the compiled companion resource pack.

#### Scenario: Merging addon models and textures
- **WHEN** an addon containing `assets/custom/models/item/wand.json` is enabled
- **THEN** the compiled resource pack output includes `assets/custom/models/item/wand.json` alongside base Croppy assets

#### Scenario: Addon asset cleanup on disable
- **WHEN** an addon is disabled or unloaded
- **THEN** its asset tree is excluded from subsequent resource pack builds without corrupting base Croppy assets
