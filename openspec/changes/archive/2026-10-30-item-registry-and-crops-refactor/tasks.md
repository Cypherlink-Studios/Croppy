# Tasks

## 1. Domain API & Core Registries

- [x] 1.1 Create `ItemDefinition` record and `ItemRegistry` port interface in `croppy-api`, and verify `.\gradlew.bat :croppy-api:compileJava` succeeds.
- [x] 1.2 Implement `DefaultItemRegistry` in `croppy-core` with thread-safe category indexing, and verify all queries and edge cases via `DefaultItemRegistryTest`.
- [x] 1.3 Implement `YamlItemLoader` in `croppy-core` to parse item YAML files from `items/<category>/*.yml` into `ItemDefinition` records, and verify with `YamlItemLoaderTest`.
- [x] 1.4 Integrate `ItemRegistry` into `CroppyCore` and `DefaultCroppyCore`, and verify all `croppy-core` tests pass with `.\gradlew.bat :croppy-core:test`.

## 2. Directory Restructuring & Configuration

- [x] 2.1 Update `YamlSpeciesLoader` in `croppy-core` to load from `crops/` directory and resolve seed item keys, and verify via updated `YamlSpeciesLoaderTest`.
- [x] 2.2 Reorganize `croppy-paper` resources: move `species/wheat.yml` to `crops/wheat.yml`, create `items/seeds/wheat_seeds.yml`, create `items/tools/inspector.yml`, and create `items/watering_cans/*.yml` (rusty, copper, silver, gold, iridium), verifying the files exist and match schemas.
- [x] 2.3 Relocate fertilizer configurations from `fertilizers/` to `items/fertilizers/` (speed_gro, retaining_soil, quality_fertilizer), adapt loader integration, and verify fertilizer definitions load cleanly.

## 3. Paper Item Service & Standardized PDC

- [x] 3.1 Implement `ItemFactory` / `PaperItemService` in `croppy-paper` to construct `ItemStack` instances from `ItemDefinition` with standardized PDC tags (`croppy:item_id`, `croppy:category`, and arbitrary PDC properties), and verify with `ItemFactoryTest`.
- [x] 3.2 Update `WateringCanService` to generate watering cans via `ItemFactory` with water usage PDC tracking (`croppy:water_uses`), and verify with `WateringCanServiceTest`.
- [x] 3.3 Update `FertilizerService` to generate items via `ItemFactory` using `croppy:item_id`, and verify with `FertilizerServiceTest`.

## 4. Command Hierarchy & Dynamic Autocompletion

- [x] 4.1 Register Cloud 2.0 suggestion providers in `CroppyCommands`: `@Suggestions("item_categories")` and contextual `@Suggestions("category_items")`, verifying suggestions correctly filter items by category.
- [x] 4.2 Implement `/croppy item give <category> <item_id> [quantity]` with `--target <player>` and `--silent` flags, plus `/croppy item list [category]` in `CroppyCommands`, and verify execution flow.
- [x] 4.3 Prune legacy give commands (`/croppy give`, `/croppy givecan`, `/croppy givefertilizer`, `/croppy tool`) from `CroppyCommands`, and verify compilation.

## 5. Listeners & Verification

- [x] 5.1 Update `CropPlantListener` to identify seeds via PDC `croppy:item_id` and match with crop species seed key, and verify planting behavior.
- [x] 5.2 Update `WateringListener`, `FertilizerListener`, and `InspectionToolListener` to check `croppy:item_id` and `croppy:category` with backward-compatible fallbacks for legacy PDC keys, and verify interaction behavior.
- [x] 5.3 Execute comprehensive test verification across all modules with `.\gradlew.bat test` and confirm zero failures.

## Workflow follow-up

- Review and approve the plan before starting the apply workflow.
- Execute `/openspec-apply-change` to implement the tasks.
- Verify, sync durable specs, and archive the completed change.
