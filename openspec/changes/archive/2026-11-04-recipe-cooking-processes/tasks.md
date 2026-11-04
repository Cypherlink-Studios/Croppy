# Tasks

## 1. API Contracts and Data Models (croppy-api)

- [x] 1.1 Create `PhaseDisplay` record in `dev.darkblade.croppy.api.process` with template and progress bar configuration (length, filled, empty).
- [x] 1.2 Create `PhaseEffects`, `PhaseParticle`, and `PhaseSound` immutable records in `dev.darkblade.croppy.api.process` for decoupled sensory feedback.
- [x] 1.3 Update `PhaseDefinition` to include optional `PhaseDisplay`, `PhaseEffects`, and `pauseOnConditionFailure` flag, preserving backward-compatible factory methods.

## 2. Core Domain Services (croppy-core)

- [x] 2.1 Implement `ProcessDisplayFormatter` in `croppy-core` resolving placeholders (`{progress_percent}`, `{progress_percent_left}`, `{progress_seconds}`, `{progress_seconds_left}`, `{progress_ticks}`, `{progress_ticks_left}`, `{progress_bar}`, `{phase_name}`, `{phase_index}`, `{phase_total}`, `{recipe_name}`, `{temperature}`).
- [x] 2.2 Add unit tests in `ProcessDisplayFormatterTest` verifying placeholder replacements and progress bar constructions.
- [x] 2.3 Implement `YamlProcessLoader` in `dev.darkblade.croppy.core.process` with flexible duration parsing (`15s`, `300t`, `2m`), start conditions, phase displays, and effects.
- [x] 2.4 Add unit tests in `YamlProcessLoaderTest` covering single-phase and multi-phase YAML process parsing and validation.
- [x] 2.5 Update `ProcessEngine` to support condition hold/pause mode during `TIMED` phases when conditions evaluate to false without immediate failure.
- [x] 2.6 Update `ProcessEngine.tryStartProcess()` and `completeProcess()` to integrate with `RecipeRegistry` and `RecipeMatcher`, binding recipe input consumption and output production.
- [x] 2.7 Add unit tests in `ProcessEngineTest` verifying linear duration progression, thermal pausing, and recipe execution lifecycle.

## 3. Paper Platform Adapter and Resources (croppy-paper)

- [x] 3.1 Create bundled process YAML definitions (`processes/tomato_soup_cook.yml`, `processes/vegetable_stew_cook.yml`, and `processes/fried_egg_cook.yml`) in `croppy-paper/src/main/resources/processes/`.
- [x] 3.2 Update `CroppyPlugin.java` with `setupProcessDefinitions()` to unpack default processes into `processes/` and load them into `ProcessRegistry`.
- [x] 3.3 Update `PaperStationTicker` to render phase holograms via `ProcessDisplayFormatter` and execute phase-defined particles and periodic sound effects.
- [x] 3.4 Add integration tests in `croppy-paper` verifying process loading and station ticker execution.
- [x] 3.5 Fix runtime process triggers and GUI synchronization: ensure ProcessComponent is restored on deserialization, enrich physical interaction context, auto-trigger idle stations with inputs in PaperStationTicker, and sync GUI inventory upon ignition.

## 4. Verification and Validation

- [x] 4.1 Execute full test suite via `./gradlew test` ensuring 100% test pass rate across all modules.
