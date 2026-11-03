# recipe-discovery Specification

## Purpose

Governs player recipe knowledge states, extensible discovery method providers, exact anti-shadowing ingredient matching, and decoupled player knowledge persistence.

## Requirements

### Requirement: Recipe Knowledge Lifecycle and States
The system SHALL track recipe knowledge per player across three distinct states: `UNKNOWN`, `HINTED`, and `DISCOVERED`, persisting acquisition timestamps and discovery method identifiers.

#### Scenario: Default unknown state
- **WHEN** a player interacts with a recipe they have never encountered or unlocked
- **THEN** their knowledge state evaluates to `UNKNOWN` with no acquisition timestamp

#### Scenario: Grant hinted knowledge
- **WHEN** a hint is unlocked for an unknown recipe via clue or partial discovery
- **THEN** the player's knowledge state transitions to `HINTED` with associated hint metadata

#### Scenario: Grant discovered knowledge
- **WHEN** a recipe discovery condition is met by a player
- **THEN** their knowledge state transitions to `DISCOVERED` and an event is emitted

#### Scenario: Idempotent discovery
- **WHEN** a discovery method attempts to grant an already discovered recipe to a player
- **THEN** the system ignores the duplicate grant and does not trigger duplicate events or notifications

### Requirement: Extensible Discovery Methods SPI
The system SHALL provide a decoupled service provider interface `RecipeDiscoveryMethod` allowing automatic, experimental, knowledge-based, world-based, and external discovery mechanisms to register and evaluate.

#### Scenario: Automatic discovery on item acquisition
- **WHEN** a player obtains an ingredient configured as an automatic discovery trigger
- **THEN** the associated recipe is unlocked immediately for that player

#### Scenario: Knowledge-based discovery prerequisites
- **WHEN** an experimental discovery requires prerequisite recipe knowledge
- **THEN** the discovery method verifies that the player already knows all prerequisite recipes before unlocking

#### Scenario: External discovery invocation
- **WHEN** an external plugin or quest system invokes the discovery API
- **THEN** the recipe knowledge is granted directly to the target player and persisted

### Requirement: Anti-Shadowing Recipe Matcher
The system SHALL evaluate station inputs against registered recipes using strict ingredient balance and specificity priority, preventing simpler subset recipes from shadowing complex combinations.

#### Scenario: Exact ingredient match priority
- **WHEN** station inputs exactly match a 3-ingredient recipe that shares 2 ingredients with a simpler recipe
- **THEN** the matcher selects the 3-ingredient recipe rather than prematurely triggering the 2-ingredient recipe

#### Scenario: Reject excess inputs in strict mode
- **WHEN** station inputs contain extra items that do not belong to the candidate recipe and the recipe does not allow excess
- **THEN** the matcher treats the combination as unmatched

### Requirement: Player Knowledge Persistence
The system SHALL persist player recipe knowledge asynchronously through an extensible `RecipeKnowledgeStorage` port, restoring knowledge upon player join and saving upon state changes.

#### Scenario: Restore player knowledge on join
- **WHEN** a player connects to the server
- **THEN** their persisted recipe knowledge records are asynchronously loaded into memory

#### Scenario: Persist discovery on state transition
- **WHEN** a player discovers a recipe during gameplay
- **THEN** the updated knowledge record is saved to the persistent storage provider without blocking the main server thread

### Requirement: Bundled Default Recipe Configurations
The system SHALL provide default bundled recipe YAML configurations in plugin resources and unpack them into the server's `recipes/` directory during startup if absent, registering them into the `RecipeRegistry`.

#### Scenario: Unpack default recipes on startup
- **WHEN** the server starts and the `recipes/` directory does not contain default recipes
- **THEN** the system unpacks `tomato_soup.yml`, `vegetable_stew.yml`, and `fried_egg.yml` from plugin resources and registers them into the `RecipeRegistry`

#### Scenario: Preserve customized recipe files
- **WHEN** a default recipe file already exists in the `recipes/` directory
- **THEN** the system preserves the existing file without overwriting it and registers its configured definition
