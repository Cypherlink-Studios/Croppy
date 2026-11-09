# Design: Clean Neutral Resources

## Context

See `proposal.md` for motivation. Croppy's bundled resources and companion resource pack require cleaning to establish a theme-agnostic baseline suitable for downstream Declarative Content Packs and Java Addons.

## Goals / Non-Goals

**Goals:**
- Provide a coherent 6-tier watering can progression following native Minecraft materials (`wooden`, `copper`, `iron`, `golden`, `diamond`, `netherite`).
- Rename fertilizer definitions to neutral agronomy nomenclature (`growth_fertilizer`, `hydrating_fertilizer`, `quality_fertilizer`).
- Standardize all bundled configuration items, names, and lore into Standard English (AGENTS.md rule 5.4).
- Delete unreferenced/orphaned assets (`hamburger.png`) and synchronize item model definitions.
- Establish standardized stage blueprints in YAML for `tomato` and `rice` ready for Blockbench 3D model delivery.
- Synchronize all unit test fixtures to ensure 100% test pass rate on `./gradlew.bat test`.

**Non-Goals:**
- Generating 3D Blockbench models and textures for tomato and rice (explicitly reserved for the user's specialized Blockbench MCP agent).
- Modifying core simulation mechanics, storage codecs, or API interfaces.

## Decisions

### 1. Watering Can Tier Hierarchy
- **Chosen Option**: Standard Minecraft tool materials:
  - `wooden_can`: 20 capacity, 1x1 area, material `WOODEN_HOE`.
  - `copper_can`: 50 capacity, 1x3 area, material `COPPER_INGOT`.
  - `iron_can`: 100 capacity, 1x3 area, material `IRON_INGOT`.
  - `golden_can`: 250 capacity, 3x3 area, material `GOLD_INGOT`.
  - `diamond_can`: 500 capacity, 3x3 area, material `DIAMOND`.
  - `netherite_can`: 1000 capacity, 5x5 area, material `NETHERITE_INGOT`.
- **Alternatives Considered**: Generic tiers (`tier_1`, `tier_2`) or Stardew progression (`rusty`, `silver`, `iridium`). Rejected because Minecraft players expect vanilla materials.

### 2. Fertilizer Nomenclature
- `speed_gro` -> `growth_fertilizer` (Speed modifier 1.25x).
- `retaining_soil` -> `hydrating_fertilizer` (Water retention effect).
- `quality_fertilizer` -> retained as `quality_fertilizer` (Quality boost 0.20).
- All display names and lores translated to Standard English MiniMessage.

### 3. Resource Pack Structure & Plant Stage Namespacing
- Crop stages follow the standardized structure:
  - `assets/croppy/items/plant/<species>/stage_<index>.json`
  - `assets/croppy/models/block/plant/<species>/stage_<index>.json`
  - `assets/croppy/textures/block/plant/<species>/stage_<index>.png`
- For `croppy:grapes`, synchronize `items/ingredients/grapes.yml` with `croppy:grapes`.

### 4. Language & Lore Neutrality
- `carrot.yml` translated from `Zanahoria` to `Carrot`.
- `inspector.yml` translated from `Herramienta de Inspección` to `Crop Inspector`.
- Remove fantasy/mystic flavor text ("energía mística") across all tools.

## Risks / Trade-offs

- **[Risk] Test breakage due to renamed keys**: Existing test suites assert `Key.of("croppy", "speed_gro")`, `Key.of("croppy", "retaining_soil")`, etc.
  - *Mitigation*: Update all corresponding test files and fixtures in `croppy-api`, `croppy-core`, and `croppy-paper`.
- **[Risk] Missing models for tomato/rice until Blockbench agent finishes**:
  - *Mitigation*: The crop definitions in `crops/tomato.yml` and `crops/rice.yml` specify the exact model IDs (`croppy:plant/tomato/stage_*` and `croppy:plant/rice/stage_*`). Item model JSONs will be set up with valid placeholders dispatching to base items until the Blockbench agent exports custom geometries.
