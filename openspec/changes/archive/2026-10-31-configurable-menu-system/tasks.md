# Tasks: Configurable Menu System

## 1. API Contracts and Domain Models (`croppy-api`)

- [x] 1.1 Implement `SlotType` enum (`INPUT`, `OUTPUT`, `STATUS`, `ACTION`, `DECORATION`) and `MenuActionType` enum in `dev.darkblade.croppy.api.menu` and verify clean compilation.
- [x] 1.2 Implement `MenuItemTemplate`, `ConditionalTemplate`, and `MenuAction` records in `dev.darkblade.croppy.api.menu` and verify parameter validation and immutability via unit tests.
- [x] 1.3 Implement `SlotDefinition`, `MenuDefinition`, and `MenuRegistry` port interface in `dev.darkblade.croppy.api.menu` and verify record construction and immutability via unit tests.
- [x] 1.4 Update `StationDefinition` to include `Key menuKey` (with default fallback to `croppy:default_station`), update constructor overloads, and verify clean compilation across all modules.

## 2. Core YAML Loading, Validation, and Registry (`croppy-core`)

- [x] 2.1 Implement `DefaultMenuRegistry` using thread-safe storage and verify registration, lookup, and unmodifiable views via JUnit 5 unit tests.
- [x] 2.2 Implement `YamlMenuLoader` using SnakeYAML to parse 9xN pattern matrices, element mappings, and slot definitions, and verify matrix dimension validation via JUnit 5 unit tests.
- [x] 2.3 Implement dynamic condition evaluation helper linking `ConditionalTemplate` expressions to `EvaluationContext` and verify conditional template resolution via JUnit 5 unit tests.
- [x] 2.4 Update `YamlStationLoader` to parse the optional `menu` key into `StationDefinition` and verify loading with both explicit and default menu keys via unit tests.

## 3. Paper Presentation, Anti-Dupe Security, and Ticker (`croppy-paper`)

- [x] 3.1 Implement `PaperMenuHolder` and `PaperMenuRenderer` to construct Adventure MiniMessage inventories, resolve custom model data and item meta, and bind storage inputs/outputs.
- [x] 3.2 Implement `PaperMenuListener` with strict anti-duplication security (pickup-only outputs, drag restrictions, number/offhand swap blocking, and safe shift-click routing to `INPUT` only) and verify slot interaction logic via unit tests.
- [x] 3.3 Implement `PaperMenuTicker` for lightweight periodic refreshes of `STATUS` slots on open inventories during active processes without cursor reset.
- [x] 3.4 Wire `PaperMenuService`, register listeners and tickers in `CroppyPlugin`, and replace hardcoded `StationGuiListener` logic in `StationInteractListener`.
- [x] 3.5 Create default YAML configurations in `menus/default_station.yml`, `menus/stations/cooking_pot.yml`, and `menus/stations/skillet.yml`, update station YAMLs with explicit `menu` keys, and verify the entire test suite passes via `./gradlew test`.
