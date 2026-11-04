# Design: Recipe Cooking Processes

## Context

Croppy features modular stations with dual interaction modes (in-world item clicks and GUI menus), persistent spatial tracking, thermal evaluation, and a rule engine. `RecipeDefinition` specifies inputs, outputs, and a `process` key, while `ProcessEngine` provides a finite state machine for executing multi-phase recipes. However, process definitions currently exist only in memory without a YAML loader or configuration directory (`processes/`), and recipes cannot automatically trigger processes with customizable cooking durations, holograms, and multi-sensory feedback.

See `proposal.md` for motivation and `specs/sequential-processes/spec.md` for requirements.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated YAML configuration loader (`YamlProcessLoader`) reading `processes/*.yml` files into `ProcessRegistry`.
- Connect `RecipeMatcher` and `RecipeDefinition` with `ProcessEngine.tryStartProcess()`, automatically binding recipe inputs and outputs to active process instances.
- Implement linear cooking time ticking with thermal pause handling when minimum temperature requirements are temporarily unmet.
- Provide a comprehensive, 100% customizable display formatting engine (`ProcessDisplayFormatter`) supporting direct and inverted (`_left`) placeholders, elapsed/remaining seconds, and configurable progress bars.
- Enable phase-level sensory feedback (ambient particles and periodic sound loops) in `PaperStationTicker`.
- Bundle default process files for `croppy:tomato_soup_cook`, `croppy:vegetable_stew_cook`, and `croppy:fried_egg_cook`.

**Non-Goals:**
- Dynamic thermal speed multipliers (thermal cooking rate) — intentionally kept to linear time for this release.
- Item quality resolution modifiers (Gold/Silver stars) — deferred to future phase.
- Interactive recipe discovery mini-games outside the existing station experimentation system.

## Decisions

### 1. Decoupled Architecture: Recipe vs Process
- **Decision**: Keep `RecipeDefinition` (crafting knowledge, inputs, discovery, outputs) separate from `ProcessDefinition` (simulation phases, durations, conditions, sensory feedback).
- **Rationale**: Follows Croppy's hexagonal architecture. A recipe defines *what* is crafted and unlocked, while a process defines *how* the station executes the transformation. Multiple recipes can share generalized process templates (e.g. `croppy:standard_pot_simmer`), or use bespoke multi-step processes.
- **Alternatives Considered**: Inlining processes directly inside recipe YAML files. Rejected because artisan machines (fermentation barrels, smokers) execute processes without recipe discovery, and duplicate phases would clutter recipe files.

### 2. Linear Time Progression and Thermal Hold
- **Decision**: Simulation ticks advance elapsed phase ticks by `deltaTicks` linearly when phase conditions (e.g., `station.temperature >= 75`) evaluate to true. When conditions evaluate to false and condition pause is active (`on_condition_pause: true` or default thermal hold), elapsed duration is frozen rather than failing immediately.
- **Rationale**: If a player's heat source runs out of fuel or temperature dips temporarily, realistic cooking pauses rather than abruptly ruining the food immediately. The existing `rules/overcooking_burn.yml` rule remains responsible for burning food if left unattended at high temperatures.
- **Alternatives Considered**: Immediate phase failure on temperature drop. Rejected as excessively punishing and frustrating for gameplay.

### 3. Rich Placeholder Engine (`ProcessDisplayFormatter`)
- **Decision**: Implement `ProcessDisplayFormatter` in `croppy-core` to resolve status holograms from phase display templates.
- **Supported Tokens**:
  - `{progress_percent}`: Current phase completion (0 to 100).
  - `{progress_percent_left}`: Inverted remaining percentage (100 to 0).
  - `{progress_seconds}`: Elapsed phase seconds.
  - `{progress_seconds_left}`: Remaining phase seconds.
  - `{progress_ticks}`: Elapsed ticks.
  - `{progress_ticks_left}`: Remaining ticks.
  - `{progress_bar}`: Visual bar constructed from configured length, filled symbol, and empty symbol.
  - `{phase_name}`: Display name of the active phase.
  - `{phase_index}` / `{phase_total}`: 1-indexed phase progression numbers.
  - `{recipe_name}`: Display name of the active recipe.
  - `{temperature}`: Formatted station temperature.
- **Rationale**: Completely fulfills user specification for 100% customizability including inverted `_left` variants.

### 4. Phase Effects and Display Data Models in `croppy-api`
- **Decision**: Add immutable records in `croppy-api`:
  - `PhaseDisplay(String template, int barLength, String barFilled, String barEmpty)`
  - `PhaseEffects(PhaseParticle particle, PhaseSound sound)`
  - `PhaseParticle(String type, int count, double offsetX, double offsetY, double offsetZ, double speed)`
  - `PhaseSound(String sound, int intervalTicks, float volume, float pitch)`
- **Rationale**: Keeps `croppy-api` pure Java 21 without importing Bukkit/Paper types (`org.bukkit.Particle`, `Sound`). The adapter in `croppy-paper` parses and executes the Bukkit-equivalent visual effects.

### 5. Time Duration Parsing
- **Decision**: In `YamlProcessLoader`, parse duration and timeout strings supporting integer ticks or formatted time strings (e.g. `15s` → 300 ticks, `2m` → 2400 ticks, `300` / `300t` → 300 ticks).
- **Rationale**: Greatly improves developer and server owner ergonomics when authoring recipes and processes.

## Risks / Trade-offs

- **[Risk] Rapid Hologram Updates causing Packet Traffic**:
  - *Mitigation*: Virtual entity metadata packets are only dispatched when the formatted status string changes or `display.isDirty()` is true. Station ticking runs at 20 ticks (1s) or configured interval, avoiding spam.
- **[Risk] Missing Process Definitions referenced by Recipes**:
  - *Mitigation*: `YamlRecipeLoader` and `ProcessEngine` log descriptive warnings if a recipe references an unknown `processKey`. If missing, `ProcessEngine` can synthesize a fallback timed process to prevent stalled stations.
- **[Risk] Station Destruction during Active Process**:
  - *Mitigation*: Covered by existing station destruction listeners: storage drops all inputs/outputs, process instance is cleared, and virtual display entities are untracked.
