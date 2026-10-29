# Tasks: Crop Instance Tracking & Lifecycle Management

## 1. Domain Spatial & Instance Contracts

- [x] 1.1 Implement `BlockPosition` record in `dev.darkblade.croppy.api.world.BlockPosition` with world name and integer coordinates.
- [x] 1.2 Implement `CropInstance` interface in `dev.darkblade.croppy.api.crop.CropInstance` modeling species key, position, stage index, progress, and timestamps.

## 2. Core CropManager Service & Unit Tests

- [x] 2.1 Implement `DefaultCropInstance` and thread-safe `CropManager` service in `dev.darkblade.croppy.core.crop`.
- [x] 2.2 Wire `CropManager` into `CroppyCore` lifecycle and service registry.
- [x] 2.3 Add JUnit 5 unit tests in `croppy-core` testing crop registration, duplicate rejection, spatial lookups, stage advancement, and removal.

## 3. Paper Events & Platform Listeners

- [x] 3.1 Implement custom Paper events in `dev.darkblade.croppy.platform.paper.event`: `CropPlantEvent`, `CropHarvestEvent`, and `CropBreakEvent`.
- [x] 3.2 Implement `CropPlantListener` in `croppy-paper` detecting registered seed placement, firing `CropPlantEvent`, and registering `CropInstance`.
- [x] 3.3 Implement `CropGrowthListener` in `croppy-paper` cancelling vanilla `BlockGrowEvent` on Croppy-tracked blocks.
- [x] 3.4 Implement `CropBreakListener` in `croppy-paper` handling mature harvests (dropping YAML yields), immature breaks, and unregistering instances.
- [x] 3.5 Implement `CropPhysicsListener` in `croppy-paper` handling water washing, piston movement, and farmland trampling.
- [x] 3.6 Register listeners in `CroppyPlugin.onEnable()` and verify full build and packaging via `gradle build`.
