# Proposal

## Why

Currently, variables in Croppy stations are monolithic, hardcoded to culinary/cooking concepts (`recipe_name`, `process_state`, `progress_percent`), and scattered across inconsistent naming conventions (`temperature` in states vs `station.temperature` in rules vs `{temperature_c}` in text displays). Furthermore, stations that do not cook food (such as heat-emitting stoves, dehydration racks, or future non-culinary machines) and non-station entities (such as crops) cannot easily expose their own state or store arbitrary custom variables without writing custom Java code.

Introducing a generic, neutral, component-based variable exposure system combined with an extensible typed variable store allows any station, crop, or addon entity to declare its state, define arbitrary variables (`var.<name>`), execute type-safe operations, and seamlessly feed into declarative states, rules, and holographic text displays.

## What Changes

- **Component Variable Providers**: Add `VariableProvider` to `croppy-api`, allowing attached components (`ThermalComponent`, `HeatSourceComponent`, `StorageComponent`, `ProcessComponent`, and addon components) to independently export variables under dedicated namespaces (`thermal.*`, `heat.*`, `storage.*`, `process.*`) with both raw values (for AST calculations) and formatted strings (for UI/displays).
- **Typed Variable Store (`var.<name>`)**: Introduce an extensible typed variable storage subsystem on `StationInstance` (and future `CropInstance`) supporting initial values via `default_variables` in configuration files.
- **Extensible Data Types & Registries**: Provide a `DataTypeRegistry` supporting `SimpleDataType<T>` (numbers, booleans, strings) and `CompoundDataType<T>` (composite, indexed, and custom addon structures), complete with binary codecs for SQLite and PDC persistence.
- **Type-Safe Variable Operations**: Introduce `VariableOperation<T>` where each data type strictly declares supported mutations (e.g. `SET`, `ADD`, `CLAMP`, `TOGGLE`, `DRAIN`), enforced fail-fast by interaction and rule systems.
- **Resilient Persistence**: Provide fallback preservation (`UnresolvedBinaryVariable`) so that if an addon providing custom data types is uninstalled, raw bytes are safely preserved without corrupting the chunk or station persistence.
- **Unified Holographic Text Formatting**: Replace the hardcoded 13-parameter `StationPlaceholderFormatter` with a contextual token evaluator resolving `{namespace.property}` and `{var.property}` directly from the unified `EvaluationContext`.
- **Consistent AST Variable Namespaces**: Align AST expressions across reactive station states (`when`), the rule engine (`RuleEngine`), and interactions to use unified namespaced paths (e.g., `thermal.temp`, `heat.fuel_ticks`, `process.state`, `var.cleanliness`).

## Capabilities

### New Capabilities
- `typed-variable-system`: Manages extensible typed variable definitions, registry of simple and compound data types, type-safe mutation operations, and resilient binary persistence for domain entities.

### Modified Capabilities
- `crafting-stations`: Updates reactive station state evaluation (`when` conditions) and component initialization to consume modular component variable providers and the typed `var.*` store.
- `station-virtual-displays`: Updates templated text holograms to dynamically resolve tokens from the unified `EvaluationContext`, supporting namespaced component variables and compound/simple `var.*` paths.

## Impact

- **API (`croppy-api`)**: New interfaces `VariableProvider`, `DataType<T>`, `SimpleDataType<T>`, `CompoundDataType<T>`, `VariableOperation<T>`, `TypedVariableStore`, `TypedVariable<T>`.
- **Core (`croppy-core`)**: Implementation of `DefaultDataTypeRegistry`, standard types (`croppy:integer`, `croppy:decimal`, `croppy:boolean`, `croppy:string`, `croppy:compound`), `VariableStoreBinaryCodec`, updated `StationStateEvaluator`, and a new contextual `ContextualTextFormatter`.
- **Paper Adapter (`croppy-paper`)**: Updating `PaperStationTicker`, `StationViewerTracker`, and `StationInteractListener` to feed and consume the unified `EvaluationContext`.
- **Configurations**: Existing station files (`cooking_pot.yml`, `skillet.yml`, `stove.yml`) will adopt clean namespaced expressions and can declare `default_variables`.
- **Backward Compatibility**: Existing aliases (e.g., `temperature` as alias to `thermal.temp`, `fuel_ticks` to `heat.fuel_ticks`) will be supported during state evaluation to prevent breaking existing configuration files.
