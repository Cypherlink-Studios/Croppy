# Design

## Context

Croppy follows a strict Hexagonal Architecture across `croppy-api` (zero dependencies), `croppy-core` (pure Java 21 domain engine), and `croppy-paper` (Paper 1.21.8 adapter). Previously, station evaluation relied on a monolithic method (`StationStateEvaluator.buildContext`) that hardcoded properties for culinary cooking stations (e.g., `recipe_name`, `process_state`, `temperature`), and text displays used a 13-argument static formatter (`StationPlaceholderFormatter`). Non-cooking stations (such as heat sources like `croppy:stove`) and future entities had no clean way to expose state or store custom variables.

See `proposal.md` for overall motivation.

## Goals / Non-Goals

**Goals:**
- Provide a clean `VariableProvider` port in `croppy-api` allowing components to independently export variables to an `EvaluationContext` under clear namespaces (`thermal.*`, `heat.*`, `storage.*`, `process.*`).
- Implement an extensible typed variable storage system (`TypedVariableStore`) under the `var.` namespace prefix for stations, supporting initial values via `default_variables` in configuration.
- Support both atomic simple data types (`SimpleDataType<T>`) and indexed composite compound types (`CompoundDataType<T>`), extensible by addons via `DataTypeRegistry`.
- Enforce strict type-safe mutations via `VariableOperation<T>`, ensuring operations (e.g., `ADD`, `TOGGLE`, `DRAIN`) are declared by data types and validated fail-fast.
- Provide forward-compatible binary serialization with fallback preservation (`UnresolvedBinaryVariable`) if an addon data type is uninstalled.
- Replace the rigid `StationPlaceholderFormatter` with a contextual token evaluator resolving `{namespace.property}` and `{var.property}` directly from the `EvaluationContext`.
- Retain backward compatibility for existing un-prefixed station variable names (`temperature`, `fuel_ticks`, etc.).

**Non-Goals:**
- Overhauling crop growth tickers to consume custom variables in this specific change (crops will gain access to the API interfaces, but full crop simulation refactoring is a future change).
- Replacing the Pratt parser / AST tokenizer in `croppy-core` (the tokenizer already supports dot-notation in identifiers).

## Decisions

### Decision 1: Component Variable Providers via `VariableProvider` Interface
- **Choice**: Add `VariableProvider` directly in `croppy-api` implemented by components (`ThermalComponent`, `HeatSourceComponent`, `StorageComponent`, `ProcessComponent`, `TypedVariableStore`).
- **Rationale**: Keeps `croppy-core` completely decoupled from specific component types. Any component (including third-party addon components) can export variables without requiring core registration or reflection.
- **Alternatives Considered**: An external extractor registry in `croppy-core`. Rejected because it would require manual registration for every component and limit addon autonomy.

### Decision 2: Distinct Raw and Formatted Variable Twins
- **Choice**: Variable providers register both raw numeric/boolean values and human-readable formatted strings under explicit suffixes (e.g., `thermal.temp`: `180.5`, `thermal.temp_c`: `"180.5°C"`; `process.progress`: `0.6`, `process.progress_bar`: `"[■■■□□]"`).
- **Rationale**: Allows AST expression evaluators (`when`, rules) to work with native numbers/booleans while text displays and action bars can directly bind to clean formatted strings without complex template filter parsing.
- **Alternatives Considered**: Liquid/Jinja-like filter syntax in template strings (`{thermal.temp | format: "%.1f°C"}`). Rejected for performance overhead and unnecessary template complexity.

### Decision 3: Type-Safe Mutation Operations via `VariableOperation<T>`
- **Choice**: Variable mutations must specify an operation identifier declared by the variable's `DataType<T>`.
- **Rationale**: Prevents illegal state transitions (such as subtracting from a string or toggling a number). Rules and interactions invoke `store.apply(key, operation, args)`, which validates the operation before mutating state.
- **Alternatives Considered**: Raw untyped `set(key, Object)` and script evaluation. Rejected because it bypasses compile-time and runtime safety, leading to subtle state corruption in SQLite and PDC.

### Decision 4: Resilient Binary Codec with Unresolved Variable Preservation
- **Choice**: Binary serialization stores `[variableKey, typeKey, payloadBytes]`. If `typeKey` is not found in `DataTypeRegistry` during deserialization, it is loaded into memory as `UnresolvedBinaryVariable(typeKey, payloadBytes)`.
- **Rationale**: If a server operator removes or temporarily disables an addon, stations containing addon-specific variables will continue running normally without crashing chunk loads or dropping variable data upon save.
- **Alternatives Considered**: Throwing exceptions on missing types, or discarding unrecognized variables. Rejected because discarding causes data loss, and exceptions crash server chunk loads.

### Decision 5: Unified Contextual Text Formatter
- **Choice**: Replace `StationPlaceholderFormatter`'s hardcoded replacement logic with a token scanner that extracts tokens enclosed in `{...}` and looks them up in the `EvaluationContext`.
- **Rationale**: Any variable in the context (component metrics, `var.*` properties, player telemetry) can be rendered in text holograms without modifying Java code.

## Risks / Trade-offs

- **[Risk: Breaking existing YAML configuration expressions]**
  - *Mitigation*: The context builder includes legacy aliases (`temperature` -> `thermal.temp`, `fuel_ticks` -> `heat.fuel_ticks`, `has_inputs` -> `storage.has_inputs`) alongside namespaced variables. Existing YAML configs continue to work seamlessly.
- **[Risk: Memory overhead from variable stores on idle stations]**
  - *Mitigation*: `TypedVariableStore` is initialized lazily; if a station definition specifies no `default_variables` and no custom variables are set, a lightweight empty store is used.
- **[Risk: Performance of regex-based token replacement in text displays]**
  - *Mitigation*: Use single-pass character scanning or cached token positions rather than repetitive regex replacements on every tick.
