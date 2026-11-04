# Proposal

## Why

Culinary recipes in Croppy define input ingredients, stations, and output items, and reference a process key (`process: Key`), but currently lack a dedicated YAML configuration loader for process definitions and an integrated cooking timer system. To enable rich culinary gameplay with single-phase and multi-step cooking, stations require a robust linear cooking duration engine with thermal condition pause handling, customizable status holograms supporting direct and inverted (`_left`) placeholders with visual progress bars, and phase-specific audiovisual feedback.

## What Changes

- Implement `YamlProcessLoader` in `croppy-core` to discover and parse `processes/*.yml` configurations into `ProcessDefinition` and `PhaseDefinition` instances, registering them into `ProcessRegistry`.
- Bundle default culinary process definitions (`tomato_soup_cook.yml`, `vegetable_stew_cook.yml`, `fried_egg_cook.yml`) in `croppy-paper` and unpack them during plugin initialization.
- Integrate `RecipeMatcher` and `RecipeDefinition` with `ProcessEngine.tryStartProcess()`, automatically resolving the recipe's bound process definition, consuming recipe inputs, and mapping recipe outputs upon completion.
- Implement linear time progression in `ProcessEngine.tick()` with support for condition-based pause states (`on_condition_pause: true` or default thermal hold) when minimum temperature thresholds are temporarily unmet.
- Implement a comprehensive placeholder and progress bar resolution engine (`ProcessDisplayFormatter`) supporting `{progress_percent}`, `{progress_percent_left}`, `{progress_seconds}`, `{progress_seconds_left}`, `{progress_ticks}`, `{progress_ticks_left}`, `{progress_bar}`, `{phase_name}`, `{recipe_name}`, and `{temperature}`.
- Add phase-level multi-sensory feedback support (`PhaseEffects`) allowing custom particle spawns and periodic sound triggers during active cooking phases.

## Capabilities

### Modified Capabilities
- `sequential-processes`: Introduce YAML process loading, recipe-to-process orchestration, linear cooking time progression with thermal pausing, customizable display placeholder and progress bar formatting, and phase-level audiovisual effect loops.

## Impact

- **API (`croppy-api`)**: Extend `PhaseDefinition` to support display formatting models and phase effect specifications.
- **Core (`croppy-core`)**: Add `YamlProcessLoader` and `ProcessDisplayFormatter`. Update `ProcessEngine` to handle recipe integration, linear duration ticking, and condition pause handling.
- **Paper (`croppy-paper`)**: Unpack default process YAML files, initialize `YamlProcessLoader` in startup sequence, and wire phase-specific particles and sounds into the station ticking loop.
- **Resources**: Add `processes/tomato_soup_cook.yml`, `processes/vegetable_stew_cook.yml`, and `processes/fried_egg_cook.yml`.
