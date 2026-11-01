# Design: Recipe Discovery and Culinary Compendium

## Context

Croppy is structured as a Hexagonal Architecture (Ports and Adapters) across three modules:
- `croppy-api`: Pure Java 21, zero external dependencies. Defines ports, records, and identifiers.
- `croppy-core`: Pure Java 21 domain simulation (`ProcessEngine`, `CropManager`, etc.) with SnakeYAML.
- `croppy-paper`: Paper 1.21.8+ platform adapter handling Bukkit events, PDC, Cloud Paper commands, and `menu-system` GUI views.

Currently, `ProcessEngine` executes recipes directly without knowledge tracking or discovery checks. This design establishes an independent discovery subsystem while keeping execution engines isolated.

## Goals / Non-Goals

**Goals:**
- Provide a clear separation: `Recipe (Content)` vs `Recipe Knowledge (Player State)` vs `Discovery Engine (Learning Mechanics)` vs `Execution Engine (Physical Cooking)`.
- Support multiple independent discovery paths via an extensible `RecipeDiscoveryMethod` SPI.
- Prevent recipe subset shadowing via strict ingredient count matching and specificity ranking.
- Ensure zero ingredient loss during experimental discovery.
- Deliver an intuitive in-game compendium GUI (`recipes_menu.yml`) integrated with Croppy's `menu-system`.
- Provide zero-database-dependency player persistence using Paper's `PersistentDataContainer` (PDC).

**Non-Goals:**
- Implement a cooking mastery/leveling RPG system (out of scope for v0.1).
- Replace or alter existing `ProcessEngine` finite-state machine mechanics.
- Support real-time web map synchronization or multi-server cross-sync (deferred to future database adapters).

## Decisions

### 1. Domain Separation: RecipeDefinition vs ProcessDefinition
* **Decision**: Introduce `RecipeDefinition` in `croppy-api` as a domain record modeling culinary recipes, category, hint configurations, and station bindings, which references or links to execution `ProcessDefinition`.
* **Rationale**: Keeps presentation and discovery concerns out of the physical simulation engine (`ProcessEngine`), honoring Hexagonal principles.
* **Alternatives considered**: Adding discovery fields directly into `ProcessDefinition`. Rejected because not all processes (e.g., automated composters, tannin extractors) are player-discovered culinary recipes.

### 2. Player Knowledge States and Immutability
* **Decision**: Define enum `KnowledgeState` with `UNKNOWN`, `HINTED`, and `DISCOVERED`, and represent player status via immutable record `PlayerRecipeKnowledge(UUID playerId, Key recipeKey, KnowledgeState state, Instant discoveredAt, Key discoveryMethodKey, Map<String, String> metadata)`.
* **Rationale**: Records guarantee immutability, thread safety, and clean binary/PDC serialization.

### 3. Pluggable Discovery SPI
* **Decision**: Define `RecipeDiscoveryMethod` in `croppy-api`:
  ```java
  public interface RecipeDiscoveryMethod {
      Key getKey();
      DiscoveryResult evaluate(DiscoveryContext context, RecipeDefinition recipe);
  }
  ```
  with built-in implementations in `croppy-core`:
  - `AutomaticDiscoveryMethod`: Triggers when an ingredient is acquired or harvested.
  - `ExperimentalDiscoveryMethod`: Evaluates station input combinations.
  - `KnowledgeBasedDiscoveryMethod`: Requires prerequisite discovered recipes.
  - `WorldBasedDiscoveryMethod`: Triggered by reading recipe scrolls/books.
  - `ExternalDiscoveryMethod`: API endpoint for third-party quests/jobs plugins.
* **Rationale**: Allows administrators to mix multiple discovery methods on a single recipe and enables third-party developers to register new triggers without modifying Croppy.

### 4. Anti-Shadowing Ingredient Matcher (RecipeMatcher)
* **Decision**: Implement a dedicated `RecipeMatcher` in `croppy-core` using two-stage evaluation:
  1. *Strict Balance Filter*: Station inputs must contain all required items without extraneous unaccounted items (unless `allow_excess: true`).
  2. *Specificity Score*: When multiple recipes match, candidate with highest number of distinct ingredients and required amounts takes precedence.
* **Rationale**: Prevents common recipes (e.g. 1 Flour + 1 Egg) from shadowing complex preparations (e.g. 1 Flour + 1 Egg + 1 Sugar).

### 5. Station Experimentation UX & Zero-Loss Policy
* **Decision**: When a player tests an undiscovered combination in a station:
  - If valid and discoverable, the system fires `RecipeDiscoveredEvent`, saves knowledge, emits chime/particles, and updates the station action button from `★ Experiment` to `Cook`.
  - Ingredients remain in the input slots; no items are consumed until the player deliberately executes the cooking process.
* **Rationale**: Eliminates accidental loss of rare ingredients caused by misclicks or trial-and-error tests.

### 6. Persistence: PDC Adapter with Thread-Safe Cache
* **Decision**: Implement `RecipeKnowledgeStorage` port with `PdcRecipeKnowledgeStorage` in `croppy-paper`.
  - Player knowledge is cached in-memory in `DefaultRecipeDiscoveryEngine` using `ConcurrentHashMap<UUID, Map<Key, PlayerRecipeKnowledge>>`.
  - On `PlayerJoinEvent`, knowledge is loaded asynchronously from the player's PDC.
  - On discovery/state change, the in-memory cache is updated immediately (for fast synchronous GUI lookups) and an asynchronous task writes the serialized binary payload to the player's PDC.
  - On `PlayerQuitEvent`, data is flushed and the player's cache entry is evicted.
* **Rationale**: Fast main-thread reads, non-blocking writes, zero database configuration required.

### 7. Paginated Compendium Menu Integration
* **Decision**: Implement `CompendiumMenuController` driving `recipes_menu.yml` via Croppy's `menu-system`:
  - Recognizes dynamic recipe slot placeholders (e.g. slot role `RECIPE_GRID`).
  - Supports category filters (`ALL`, `BAKERY`, `SOUPS`, `BEVERAGES`, `PRESERVES`).
  - Shows green/red availability indicators based on real-time viewer inventory contents.

## Risks / Trade-offs

- **[Risk: Heavy matching overhead during frequent station clicks]** → *Mitigation*: The `RecipeMatcher` executes only when the player clicks the experiment button or updates station inputs, never during recurring tick loops.
- **[Risk: Player quit before async PDC flush finishes]** → *Mitigation*: On player quit, write synchronously or block until the pending PDC buffer is committed before clearing session memory.
- **[Risk: Recipe deleted or renamed in YAML configuration]** → *Mitigation*: The loader retains orphaned discovery records gracefully without crashing, logging a warning if an unknown key is encountered.

## Migration Plan

1. Pure API contracts and records created in `croppy-api`.
2. Discovery engine, matchers, and YAML loaders implemented in `croppy-core` with complete JUnit 5 test coverage.
3. Paper adapters (`PdcRecipeKnowledgeStorage`, `StationExperimentationHandler`, Cloud commands, GUI) added to `croppy-paper`.
4. Verify with `./gradlew.bat test`.
