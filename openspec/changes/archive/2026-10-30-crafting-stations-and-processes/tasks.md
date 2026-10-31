# Tasks

## 1. Condition & Expression Engine

- [x] 1.1 Define `Expression`, `EvaluationContext`, and `VariableResolver` interfaces in `croppy-api` and verify compilation
- [x] 1.2 Implement expression tokenizer and AST nodes (`ComparisonNode`, `LogicalNode`, `VariableNode`, `LiteralNode`) in `croppy-core`
- [x] 1.3 Implement Pratt / recursive-descent `ExpressionCompiler` in `croppy-core` and verify with JUnit 5 unit tests covering operators, parentheses, and arithmetic
- [x] 1.4 Implement `PlaceholderApiVariableResolver` in `croppy-paper` with graceful fallback when PlaceholderAPI is absent

## 2. Crafting Stations Foundation & Components

- [x] 2.1 Model `StationDefinition`, `StationInstance`, and component interfaces (`StorageComponent`, `ProcessComponent`, `DisplayComponent`) in `croppy-api`
- [x] 2.2 Implement thread-safe `DefaultStationManager` in `croppy-core` with spatial index by `ChunkPosition` and `BlockPosition`
- [x] 2.3 Implement YAML station definition loader in `croppy-core` and verify with unit tests
- [x] 2.4 Extend spatial persistence layer to save and restore station components across chunk unload/load

## 3. Sequential Process Runtime Engine

- [x] 3.1 Model `ProcessDefinition`, `PhaseDefinition`, `PhaseType`, `ProcessInstance`, `ProcessState`, and `OutputResolver` in `croppy-api`
- [x] 3.2 Implement multi-phase finite state machine in `croppy-core` handling `TIMED`, `MANUAL_INTERACTION`, and `ENVIRONMENTAL` transitions
- [x] 3.3 Implement phase timeout and failure policy execution (`CANCEL`, `SPOIL`, `BYPRODUCT`) in `croppy-core`
- [x] 3.4 Implement dynamic output resolver and quality grade calculation in `croppy-core` and verify with comprehensive JUnit 5 unit tests

## 4. Paper Adapters & Dual Interaction

- [x] 4.1 Implement physical in-world station interaction listener (`PlayerInteractEvent`) in `croppy-paper` supporting right-click ingredient insertion and tool interaction
- [x] 4.2 Implement virtual container GUI holder and click listener in `croppy-paper` for stations configured with virtual menus
- [x] 4.3 Implement `PaperStationTicker` in `croppy-paper` ticking running processes and updating visual display entities and particle effects on the main server thread
- [x] 4.4 Implement block placement (`BlockPlaceEvent`) and destruction (`BlockBreakEvent`) handlers ensuring proper drops and persistence cleanup

## 5. Verification & Test Suite Gate

- [x] 5.1 Execute `./gradlew.bat test` ensuring all existing crop tests and new condition/station/process test suites pass with 0 failures
