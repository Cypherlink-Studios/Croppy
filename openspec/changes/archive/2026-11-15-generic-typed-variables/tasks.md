# Tasks

## 1. API Contracts and Data Model (`croppy-api`)

- [x] 1.1 Create `VariableProvider` interface in `dev.darkblade.croppy.api.expression` and verify contract via test compilation
- [x] 1.2 Define `DataType<T>`, `SimpleDataType<T>`, `CompoundDataType<T>`, and `DataTypeRegistry` in `dev.darkblade.croppy.api.variable`
- [x] 1.3 Define `VariableOperation<T>`, `OperationArgs`, and `TypedVariable<T>` contracts in `croppy-api`
- [x] 1.4 Define `TypedVariableStore` port interface extending `VariableProvider` in `croppy-api`

## 2. Core Registries, Standard Types, and Operations (`croppy-core`)

- [x] 2.1 Implement `DefaultDataTypeRegistry` and standard primitive data types (`croppy:integer`, `croppy:decimal`, `croppy:boolean`, `croppy:string`)
- [x] 2.2 Implement standard `CompoundDataType` supporting nested key-value indexing and dot-notation property exports
- [x] 2.3 Implement standard operations (`SET`, `ADD`, `SUBTRACT`, `CLAMP`, `TOGGLE`, `APPEND`) with strict type validation
- [x] 2.4 Implement `DefaultTypedVariableStore` managing typed variables and exporting to `EvaluationContext` under the `var.` namespace
- [x] 2.5 Write JUnit 5 unit tests covering `DataTypeRegistry`, `TypedVariableStore`, operation execution, and type validation errors

## 3. Binary Persistence and Codecs (`croppy-core`)

- [x] 3.1 Implement `VariableStoreBinaryCodec` serializing typed variable stores as `(typeKey, payload)` binary streams
- [x] 3.2 Implement `UnresolvedBinaryVariable` fallback handling when reading unregistered addon data types
- [x] 3.3 Update `StationBinaryCodec` to serialize and deserialize the station's `TypedVariableStore`
- [x] 3.4 Write JUnit 5 unit tests for binary serialization round-trips, including preserving unresolved addon variables

## 4. Component Variable Providers and State Evaluation (`croppy-core` & `croppy-paper`)

- [x] 4.1 Implement `VariableProvider` on `ThermalComponent` exposing `thermal.temp`, `thermal.temp_c`, `thermal.zone`, and `thermal.is_heated`
- [x] 4.2 Implement `VariableProvider` on `HeatSourceComponent` exposing `heat.fuel_ticks`, `heat.is_active`, and `heat.target_temp`
- [x] 4.3 Implement `VariableProvider` on `StorageComponent` exposing `storage.has_inputs`, `storage.input_count`, `storage.has_outputs`, and `storage.is_empty`
- [x] 4.4 Implement `VariableProvider` on `ProcessComponent` exposing `process.state`, `process.progress`, `process.progress_percent`, and `process.progress_bar`
- [x] 4.5 Refactor `StationStateEvaluator.buildContext` to dynamically aggregate all `VariableProvider` components and `TypedVariableStore`, including legacy fallback aliases
- [x] 4.6 Write JUnit 5 unit tests in `StationStateEvaluatorTest` verifying state resolution with namespaced expressions, `var.*` expressions, and legacy alias expressions

## 5. Contextual Text Formatter and Hologram Rendering (`croppy-core` & `croppy-paper`)

- [x] 5.1 Implement `ContextualTextFormatter` in `croppy-core` resolving `{namespace.property}` and `{var.property}` tokens directly from `EvaluationContext`
- [x] 5.2 Refactor `StationViewerTracker` to render Text Display holograms using `ContextualTextFormatter`
- [x] 5.3 Write JUnit 5 unit tests verifying contextual text formatting across cooking pots, stoves, and custom variable tokens

## 6. YAML Loading and End-to-End Verification

- [x] 6.1 Update `YamlStationLoader` to parse `default_variables` section in station YAML configuration files
- [x] 6.2 Update `cooking_pot.yml`, `skillet.yml`, and `stove.yml` to adopt namespaced state conditions and sample default variables
- [x] 6.3 Run `.\gradlew.bat test` to verify all test suites pass with zero regressions
