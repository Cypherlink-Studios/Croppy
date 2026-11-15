# Proposal

## Why

Developer-facing Java API references, `croppy-api` contracts, classloader internals, and code examples are currently scattered across administrator and YAML configuration guides (`addon-system.md`, `workstations-configuration.md`, `rules-and-interactions.md`, `integrations.md`, `localization.md`, `growth-and-environment.md`), mixing distinct audiences and leaving many `croppy-api` contracts undocumented. Reorganizing all programmatic and developer documentation into a dedicated `docs/<lang>/api/*` subsection improves discoverability, separates server configuration from Java extension guides, and aligns documentation with the current `croppy-api` surface (`0.10.0`).

## What Changes

- Create a modular 5-page developer documentation subsection under `docs/en/api/` and `docs/es/api/` with strict 1:1 slug parity:
  - `api/index.md` (`croppy-api` overview, hexagonal boundaries, Gradle dependency setup, `Key`/`Keyed`, spatial records, package map)
  - `api/addon-development.md` (`CroppyAddon`, `AddonContext`, `AddonConfig`, `PaperAddonContext`, `AddonClassLoader` DAG, 5-phase decommissioning protocol, dormant crops, and `croppy-api` growth extension hooks)
  - `api/typed-variables.md` (`TypedVariableStore`, `DataType<T>`, `SimpleDataType<T>`, `CompoundDataType<T>`, `DataTypeRegistry`, `VariableOperation<T>`, `OperationArgs`, `VariableStoreBinaryCodec`, `UnresolvedVariableData`)
  - `api/expressions-and-providers.md` (`VariableProvider`, `EvaluationContext`, `Expression`, `VariableResolver`, component variable namespaces, Pratt AST rules/expressions, and `ContextualTextFormatter`)
  - `api/ports-and-registries.md` (`Registry<T>`, the 12 domain registries exposed by `AddonContext`, `CroppyLogger`/`LogChannel`/`LogLevel`, `ProtectionPort`, `EconomyPort`, `TranslationProvider`/`MessageKey`/`LocaleRef`)
- Refactor existing root guides in `docs/en/` and `docs/es/` (`addon-system.md`, `workstations-configuration.md`, `rules-and-interactions.md`, `integrations.md`, `localization.md`, `growth-and-environment.md`, `index.md`) to focus on server administration, gameplay, and YAML configuration, replacing embedded Java/API sections with clear cross-links to `api/*`.
- Harmonize stale version references (`0.5.7` -> `0.10.0`) and align legacy variable examples in `rules-and-interactions.md` with the v0.10.0 namespaced `VariableProvider` model.
- Coordinate the portal loader prerequisite in `DarkBladeDev-plugins` (`src/loaders/github-docs-loader.ts` `validateDocSlugs` recursive parity check for nested subdirectories such as `api/`) before deploying nested documentation.

## Capabilities

### New Capabilities
- None (`skip_specs: true` — pure documentation restructuring with no runtime plugin behavior or specification changes).

### Modified Capabilities
- None.

## Impact

- **Documentation (`docs/en/*`, `docs/es/*`)**: Adds `docs/<lang>/api/{index,addon-development,typed-variables,expressions-and-providers,ports-and-registries}.md` and updates 7 existing guides per locale with refactored sections and relative links (`./api/...` and `../...`).
- **Portal Ingestion (`DarkBladeDev-plugins`)**: Requires updating `validateDocSlugs` in `src/loaders/github-docs-loader.ts` (and corresponding unit tests in `src/loaders/github-docs-loader.test.ts`) to recurse into nested subdirectories like `api/` while skipping version folders.
- **Runtime Code (`croppy-api`, `croppy-core`, `croppy-paper`)**: Read-only reference only; no Java source files are modified.
