# Design: Species Definitions & Crop Schemas

## Context

See `proposal.md` for motivation. With the project foundation established (`croppy-api`, `croppy-core`, `croppy-paper`), we introduce the first core domain model: `Species` and its declarative schema loader.

## Goals / Non-Goals

**Goals:**
- Provide pure Java domain records and interfaces in `croppy-api` modeling crop species, stages, displays, requirements, and harvests.
- Model environmental requirements with light level and fluid level (integer 0 to 8, where 8 is water source, 1-7 flowing water, and 0 dry-farming).
- Implement a standalone YAML loader (`YamlSpeciesLoader`) in `croppy-core` using SnakeYAML, completely decoupled from Bukkit.
- Provide strict validation catching missing required fields, non-existent stages, negative progress, or invalid fluid levels.
- Provide default configurations for vanilla crops (`wheat.yml`, `carrot.yml`, `potato.yml`) in `croppy-paper`.
- Wire default file extraction and registry loading in `CroppyPlugin.onEnable()`.
- Validate parsing and validation rules with headless JUnit 5 unit tests.

**Non-Goals:**
- No world block tracking or planting interaction listeners (`CropInstance` and plant events are deferred to `crop-instance-and-planting`).
- No growth calculation or tick simulation (deferred to `growth-simulation-engine`).

## Decisions

### 1. `display` Terminology over `representation`
- **Decision:** Use `display` in both the species root and stage blocks:
  ```yaml
  display:
    type: "vanilla"
    block: "minecraft:wheat"
    seed_item: "minecraft:wheat_seeds"
  ```
- **Rationale:** More direct, intuitive, and standard for visual mapping compared to `representation`.

### 2. Water Requirement as Fluid Level (0–8)
- **Decision:** Replace boolean `requires_hydrated_soil` with `water_fluid_level: <int>`:
  - `8`: Requires full water source block (`FluidState.isSource()`).
  - `1–7`: Accepts flowing water down to the specified fluid depth.
  - `0` (or omitted): Dry-farming crop requiring no water.
- **Rationale:** Directly maps to Minecraft's actual `FluidState` block data (levels 1 to 8), allowing differentiated crop requirements (e.g. thirsty crops vs rustic crops vs desert crops).

### 3. File-per-Species Layout (`species/*.yml`)
- **Decision:** Load species from individual files in `plugins/Croppy/species/`.
- **Rationale:** Modular and clean for server administrators to inspect, add, or disable individual crops without risking syntax errors in a monolith config.

### 4. SnakeYAML in `croppy-core`
- **Decision:** Add SnakeYAML (`org.yaml:snakeyaml:2.2`) to `croppy-core`.
- **Rationale:** Allows full YAML parsing and schema validation in pure Java without importing `org.bukkit.configuration.file.YamlConfiguration`, maintaining compile-time independence from Bukkit and running in JUnit 5.

### 5. Specialized `SpeciesRegistry`
- **Decision:** `SpeciesRegistry` interface extending `Registry<Species>`, providing fast lookup maps:
  - `Optional<Species> findBySeedItem(String itemId)`
  - `Optional<Species> findByBlock(String blockId)`
- **Rationale:** Crucial for efficient event interception (e.g. checking whether an interacted block or planted seed belongs to a registered Croppy species).

## Risks / Trade-offs

- **[Malformed user configs]** → `YamlSpeciesLoader` logs descriptive validation errors naming the file, invalid key, and reason, skipping the broken crop rather than halting plugin initialization.
- **[Vanilla identifier formatting]** → Normalize all item and block identifiers to lowercase namespaced IDs (e.g. `minecraft:wheat`).
