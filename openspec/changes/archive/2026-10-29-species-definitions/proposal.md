# Proposal: Species Definitions & Crop Schemas

## Why

Croppy requires an extensible species registry to represent crops independently of vanilla Minecraft blocks. Rather than hardcoding crop behavior, agricultural properties must be defined declaratively via YAML files, allowing server administrators to customize growth stages, visual block ages, environmental thresholds (such as minimum light and water fluid levels 1-8), and harvest yields.

## What Changes

- Introduce the species domain model in `croppy-api`:
  - `Species`: Extends `Keyed`, providing identity, display representation, growth stages, environmental requirements, and harvest drops.
  - `CropDisplay`: Captures the visual backing (e.g. vanilla crop block and triggering seed item).
  - `StageDefinition`: Models abstract biological stages, required progression points, and display mapping (e.g. `block_age`).
  - `CropRequirements`: Specifies environmental growth thresholds, including minimum light level and required water fluid level (1 to 8, or 0 if dry-farming).
  - `HarvestDefinition` & `YieldEntry`: Declares items dropped upon harvest with min/max quantities.
  - `SpeciesRegistry`: Specialized registry extending `Registry<Species>` with indexed lookups by seed item and block type.
- Add SnakeYAML dependency to `croppy-core` and implement `YamlSpeciesLoader` to parse and strictly validate individual `species/*.yml` files.
- Add unit tests in `croppy-core` verifying schema validation, stage progression sanity, and error reporting on malformed YAMLs.
- Provide default bundled species definitions in `croppy-paper` for vanilla crops:
  - `species/wheat.yml` (Trigo)
  - `species/carrot.yml` (Zanahoria)
  - `species/potato.yml` (Patata)
- Wire species loading into `CroppyPlugin.onEnable()` to automatically deploy defaults to `plugins/Croppy/species/` and register them into `SpeciesRegistry`.

## Capabilities

### New Capabilities
- `species-definitions`: Declarative crop species schemas, abstract stage progression modeling, environmental thresholds (light and water fluid levels 1-8), harvest yield definitions, and validated YAML-based loading.

### Modified Capabilities
<!-- None: Builds upon existing core-registry without changing its contract -->

## Impact

- Adds SnakeYAML dependency to `croppy-core`.
- Establishes the declarative schema for all crops in Croppy.
- Enables downstream changes (`crop-instance-and-planting`, `growth-simulation-engine`) to bind planted crops and evaluate growth modifiers against registered species.
