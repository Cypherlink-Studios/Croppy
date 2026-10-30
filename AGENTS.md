# AGENTS.md — Croppy Engineering Playbook

> **Scope**: Binding design conventions, architectural boundaries, coding standards, and workflow rules for AI agents and human contributors working on the **Croppy** project.  
> **Platform & Target**: Paper 1.21.8+ (Minecraft), Java 21 (Toolchain), Gradle Kotlin DSL.

---

## 1. Project Overview & Role of Agents

**Croppy** is an extensible, modular agriculture, ingredients, processing, and cooking simulation engine for Minecraft Paper servers. It connects environmental farming conditions to crop growth, item qualities, processing stations, and culinary recipes.

As an agent operating in this repository, your primary responsibilities are:
1. **Preserve Architectural Isolation**: Never compromise modular boundaries between pure domain logic and Minecraft/Paper platform APIs.
2. **Deliver High-Quality, Modern Java 21 Code**: Emphasize records, immutability, thread-safety, and strict null safety.
3. **Follow the OpenSpec Governance Flow**: Formulate proposals, maintain synchronized specifications, and verify tasks with automated test suites before completing work.
4. **Maintain Operational Memory**: Proactively log significant design choices, architectural changes, and bugfixes into Engram memory (`mem_save`).

---

## 2. Architecture & Modular Boundaries

Croppy follows a strict **Hexagonal Architecture (Ports & Adapters)** across a 3-module Gradle structure:

```
Croppy (Root)
├── croppy-api     # Pure Java 21 Ports, Records, DTOs & Contracts (Zero Dependencies)
├── croppy-core    # Pure Java 21 Domain Engine, Simulation, Codecs & Registries (Depends only on api + SnakeYAML)
└── croppy-paper   # Paper 1.21.8 Adapter (Adapts Bukkit/Paper to Core & API Ports)
```

### Module Responsibilities & Dependency Rules

| Module | Allowed Dependencies | Prohibited Dependencies | Responsibilities |
|---|---|---|---|
| **`croppy-api`** | Standard Java 21 Library **ONLY** (Zero external dependencies). | Any external library, SnakeYAML, Bukkit, Paper, Kyori, Libby, HikariCP. | Public interfaces (Ports), domain identifiers (`Key`), spatial coordinates (`BlockPosition`, `ChunkPosition`), immutable definition records (`Species`, `StageDefinition`, `CropRequirements`). |
| **`croppy-core`** | `croppy-api`, `org.yaml:snakeyaml:2.2`, JUnit 5 (test). | Bukkit, Paper, NMS, CraftBukkit, Kyori Adventure, Libby. | Business logic, simulation engines (`GrowthEngine`), in-memory management (`CropManager`), binary serialization (`CropBinaryCodec`), YAML loading (`YamlSpeciesLoader`), domain registries. |
| **`croppy-paper`** | `croppy-api`, `croppy-core`, Paper API 1.21.8+, Cloud Paper 2.0+, Libby Paper, HikariCP, SQLite JDBC, Kyori Adventure. | Direct internal NMS without adapters. | Paper adapter implementation, Bukkit event listeners, command registration, block display synchronization, scheduled tickers, persistent storage backends (PDC & SQLite). |

### Strict Inviolable Rules
1. **Zero Bukkit in Core/API**: NEVER import `org.bukkit.*`, `io.papermc.*`, or `net.kyori.*` into `croppy-api` or `croppy-core`.
2. **Inversion of Control**: When domain logic requires platform operations (e.g., getting block light level, updating block visual displays, checking world rain), define an interface port in `croppy-api` (e.g., `EnvironmentProvider`, `DisplaySynchronizer`) and implement the adapter in `croppy-paper`.
3. **No Leaking Platform Objects**: Domain interfaces must receive and return pure types (`BlockPosition`, `CropEnvironment`, `Key`), never `org.bukkit.Location`, `World`, or `ItemStack`.

---

## 3. Concurrency & Threading Model

Paper operates on a tick-driven game loop (50ms per tick) with strict thread restrictions:

### 1. Server Main Thread (Tick Thread)
- **Mandatory for Platform State**: All reads and writes to Bukkit `World`, `Block`, `Chunk` (live blocks), `Player`, `Inventory`, and `Entity` MUST occur on the Server Main Thread.
- **Event Listeners**: Bukkit events execute synchronously on the main thread. Keep handler methods fast and non-blocking.
- **Visual Synchronization**: Calls to `DisplaySynchronizer.updateDisplay()` and block state changes must execute on the main thread.

### 2. Asynchronous Tasks & Background Workers
- **Mandatory for I/O and Persistence**: Database operations (SQLite queries via HikariCP), filesystem reads/writes, and expensive serialization tasks MUST run asynchronously via Paper's scheduler (`runTaskAsynchronously`).
- **Never Block Main Thread**: Never perform blocking JDBC queries, HTTP requests, or unbuffered disk I/O in event listeners or tick loops.

### 3. Core Thread-Safety
- **Thread-Safe State Containers**: Domain services in `croppy-core` (such as `CropManager`, `DefaultSpeciesRegistry`) may be read concurrently by background tasks, commands, or tickers. They MUST use thread-safe structures (`ConcurrentHashMap`, `CopyOnWriteArrayList`, or atomic primitives).
- **Snapshot Iteration**: Provide defensive copies or thread-safe iteration views (e.g., `cropManager.getAllCrops()` returning an unmodifiable collection view) to prevent `ConcurrentModificationException` during tick cycles.

---

## 4. Java 21 Code Conventions & Standards

### 4.1 Object Modeling: Records vs Classes
- **Use `record`** for immutable data holders, DTOs, domain coordinates, identifiers, and configuration specs:
  ```java
  public record BlockPosition(String worldName, int x, int y, int z) {}
  public record CropRequirements(int minLight, int waterFluidLevel) {}
  public record Key(String namespace, String value) implements Comparable<Key> {}
  ```
- **Use `class`** only when maintaining mutable runtime state or lifecycle services (e.g., `DefaultCropInstance`, `CropManager`, `GrowthEngine`).

### 4.2 Null-Safety & Collections
- **Pure Java Enforcement**: `croppy-api` has zero external annotation dependencies. Enforce null-safety using standard Java 21 constructs.
- **Constructor & Parameter Validation**: Validate all parameters at entry points using `Objects.requireNonNull(arg, "paramName cannot be null")`.
- **`Optional<T>` for Query Returns**: Never return `null` from public API methods when an object might be absent. Return `Optional<T>`:
  ```java
  public Optional<CropInstance> getCrop(BlockPosition position);
  ```
- **Empty Collections**: Never return or accept `null` to represent absence of items. Use `List.of()`, `Set.of()`, or `Collections.emptyList()`.
- **Immutability & Defensive Copies**: Expose immutable collections using `List.copyOf()`, `Map.copyOf()`, or `Collections.unmodifiableList()`.

### 4.3 Identifier & Key Conventions
- All domain definitions (species, fertilizers, recipes, stations) MUST be identified by a `Key`:
  - Format: `<namespace>:<value>` (e.g., `croppy:wheat`, `croppy:golden_watering_can`).
  - Values must use lowercase alphanumeric characters with underscores (`[a-z0-9_.-]+`).

### 4.4 Naming & Package Structure
- **Package Base**: `dev.darkblade.croppy`
- **Interfaces (Ports)**: Name by capability without `I` prefix (e.g., `CropStorage`, `EnvironmentProvider`).
- **Default Implementations**: Prefix with `Default` for core classes (e.g., `DefaultCropInstance`, `DefaultSpeciesRegistry`).
- **Platform Adapters**: Prefix with platform name (e.g., `PaperDisplaySynchronizer`, `SqliteCropStorage`, `PdcCropStorage`).

---

## 5. Error Handling, Logging & Player Feedback

### 5.1 Fail-Fast Initialization
- When loading definitions, registries, or configuration during startup or reload:
  - Throw informative `IllegalArgumentException` or `IllegalStateException` on duplicate keys, missing mandatory fields, or invalid numerical ranges.
  - Fail fast before the server accepts player connections so configuration bugs are caught immediately.

### 5.2 Fault-Tolerant Runtime Simulation
- During recurring tickers (`PaperGrowthTicker`, `DailyTransitionService`) or chunk event listeners:
  - Isolate crop processing in a `try-catch` block.
  - An error in a single crop or chunk must **never** terminate the simulation loop or crash the server tick.
  - Log failures with full diagnostic context (position, world, species key) at `Level.SEVERE` or `Level.WARNING`.

### 5.3 Logging Guidelines
- **No `System.out.println`**: Always use an injected `java.util.logging.Logger` (in Core) or `getComponentLogger()` / `getLogger()` (in Paper).
- In `croppy-core`, constructors must accept a `Logger` or default to `Logger.getLogger(ClassName.class.getName())`.

### 5.4 Player-Facing Messages (Kyori Adventure)
- All player-facing chat, action bar, item names, and lore MUST use **MiniMessage** formatted text (e.g., `<green>Success</green>`, `<gray>Water: <aqua><level>/8`).
- **Legacy Section Characters (`§`) are strictly prohibited.**
- Externalize all user-facing strings into `messages.yml` or configurable schemas.

---

## 6. Testing & Quality Assurance

### 6.1 Unit Test Pyramid (JUnit 5)
- All domain logic, calculations, modifiers, codecs, and state transitions in `croppy-core` MUST have comprehensive JUnit 5 unit tests.
- Strive for high test coverage on edge cases (e.g., minimum/maximum light levels, unwatered policies, binary serialization round-trips).
- Design adapter services in `croppy-paper` to depend on interfaces so they can be unit-tested without launching a full Minecraft server runtime.

### 6.2 Pre-Commit Verification Gate
Before finishing any task, checking off a checklist item, or pushing changes:
```powershell
# Must complete successfully with 0 test failures
.\gradlew.bat test
```
Agents must never leave compilation errors or broken unit tests in the workspace.

---

## 7. OpenSpec Governance & Workflow

Every functional addition, architectural adjustment, or API change must follow the OpenSpec lifecycle:

```
[Explore / Idea] ──> [OpenSpec Propose] ──> [Apply & Implement Tasks] ──> [Verify Tests] ──> [Archive & Sync Specs]
```

1. **Proposal**: Create an OpenSpec change under `openspec/changes/<change-name>` containing `proposal.md`, `design.md`, delta specs under `specs/`, and `tasks.md`.
2. **Implementation**: Execute tasks sequentially, updating `tasks.md` status checkboxes (`- [x]`) as each task is implemented and tested.
3. **Verification**: Execute `./gradlew test` and confirm all test suites pass.
4. **Archive & Sync**: Sync delta specs to durable specifications in `openspec/specs/` and move the change directory to `openspec/changes/archive/`.
5. **Memory Persistence**: Proactively record major architectural milestones and decisions to Engram persistent memory using `mem_save`.
