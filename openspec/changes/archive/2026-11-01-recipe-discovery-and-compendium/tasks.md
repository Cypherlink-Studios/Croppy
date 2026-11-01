# Tasks: Recipe Discovery and Culinary Compendium

## 1. Pure API Contracts (`croppy-api`)

- [x] 1.1 Create `KnowledgeState` enum (`UNKNOWN`, `HINTED`, `DISCOVERED`) and `PlayerRecipeKnowledge` record with parameter validation and verify compilation
- [x] 1.2 Create `RecipeDefinition`, `HintDefinition`, and `RecipeCategory` records in `dev.darkblade.croppy.api.recipe` and verify compilation
- [x] 1.3 Create `RecipeDiscoveryMethod` SPI, `DiscoveryContext`, and `DiscoveryResult` in `dev.darkblade.croppy.api.discovery` and verify compilation
- [x] 1.4 Create `RecipeKnowledgeStorage` port interface in `dev.darkblade.croppy.api.storage` and verify compilation
- [x] 1.5 Verify `croppy-api` builds cleanly with `./gradlew.bat :croppy-api:build`

## 2. Core Domain Engine (`croppy-core`)

- [x] 2.1 Implement `DefaultRecipeRegistry` in `dev.darkblade.croppy.core.recipe` and verify registration with unit tests
- [x] 2.2 Implement `RecipeMatcher` evaluating exact ingredient balance and specificity ranking, and verify with comprehensive `RecipeMatcherTest` unit tests
- [x] 2.3 Implement `DefaultRecipeDiscoveryEngine` managing discovery method registrations, thread-safe player knowledge cache, and idempotent grants with unit tests
- [x] 2.4 Implement built-in `RecipeDiscoveryMethod` providers (`AutomaticDiscoveryMethod`, `ExperimentalDiscoveryMethod`, `KnowledgeBasedDiscoveryMethod`) and unit test scenarios
- [x] 2.5 Implement `YamlRecipeLoader` parsing recipes, hint presentation rules, and discovery triggers from YAML files with unit tests in `YamlRecipeLoaderTest`
- [x] 2.6 Verify `croppy-core` test suite passes with `./gradlew.bat :croppy-core:test`

## 3. Platform Persistence & Station Integration (`croppy-paper`)

- [x] 3.1 Implement `PdcRecipeKnowledgeStorage` serializing player recipe knowledge into Paper `PersistentDataContainer` and verify unit tests
- [x] 3.2 Implement `BukkitRecipeDiscoveryListener` handling player join/quit cache lifecycle and automatic discovery triggers
- [x] 3.3 Implement `StationExperimentationHandler` integrating experimental cooking actions into crafting stations without consuming ingredients on discovery
- [x] 3.4 Implement audiovisual feedback helper for recipe discoveries emitting particles, sounds, and localized MiniMessage titles
- [x] 3.5 Verify paper persistence and station experimentation logic with unit tests

## 4. Culinary Compendium GUI & Commands (`croppy-paper`)

- [x] 4.1 Create `recipes_menu.yml` default menu configuration with dynamic recipe slots, pagination, and category buttons in `croppy-paper/src/main/resources/`
- [x] 4.2 Implement `CompendiumMenuController` rendering recipe cards based on knowledge state (`DISCOVERED`, `HINTED`, `UNKNOWN`) and real-time inventory item availability
- [x] 4.3 Register `/croppy recipes` player command and `/croppy recipe give|revoke|list` admin commands in Cloud Paper 2.0 command manager
- [x] 4.4 Add localized MiniMessage messages in English (`en-US.yml`) and Spanish (`es-ES.yml`) for discovery notifications and compendium actions
- [x] 4.5 Execute pre-commit verification gate `./gradlew.bat test` ensuring all tests pass with zero failures

## Workflow follow-up

- Review and apply tasks using `/openspec-apply-change`.
- Verify full test suite and build artifacts.
- Archive change using `/openspec-archive-change` and sync durable specs.
