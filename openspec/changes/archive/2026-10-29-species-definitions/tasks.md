# Tasks: Species Definitions & Crop Schemas

## 1. Domain API Contracts

- [x] 1.1 Define `CropDisplay` record in `dev.darkblade.croppy.api.species` modeling display type, block id, and seed item id.
- [x] 1.2 Define `StageDefinition` record in `dev.darkblade.croppy.api.species` modeling stage id, progress required, and display block age.
- [x] 1.3 Define `CropRequirements` record in `dev.darkblade.croppy.api.species` modeling minimum light and water fluid level (0 to 8).
- [x] 1.4 Define `HarvestDefinition` and `YieldEntry` records in `dev.darkblade.croppy.api.species` modeling item drop yields with min/max quantities.
- [x] 1.5 Define `Species` interface and `SpeciesRegistry` interface extending `Registry<Species>` with fast lookups (`findBySeedItem`, `findByBlock`).

## 2. YAML Loader & Core Implementation

- [x] 2.1 Add SnakeYAML dependency to `croppy-core/build.gradle.kts` and verify Gradle compilation.
- [x] 2.2 Implement `SpeciesDefinition` immutable model and `DefaultSpeciesRegistry` in `dev.darkblade.croppy.core.species`.
- [x] 2.3 Implement `YamlSpeciesLoader` in `dev.darkblade.croppy.core.species` parsing YAML streams and validating required fields, stages, and water fluid levels (0..8).
- [x] 2.4 Add JUnit 5 unit tests in `croppy-core` testing valid species YAML parsing, invalid YAML rejection, and registry lookups.

## 3. Paper Bundled Defaults & Plugin Integration

- [x] 3.1 Create default YAML definitions for vanilla crops: `species/wheat.yml`, `species/carrot.yml`, and `species/potato.yml` in `croppy-paper/src/main/resources/species/`.
- [x] 3.2 Update `CroppyPlugin.onEnable()` to deploy default species files to `<dataFolder>/species/` and load them into `SpeciesRegistry`.
- [x] 3.3 Verify full Gradle build and test execution via `gradle build`.
