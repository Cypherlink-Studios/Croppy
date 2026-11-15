# Tasks

## 1. Portal Loader Prerequisite (`DarkBladeDev-plugins`)

- [x] 1.1 Update `validateDocSlugs` in `DarkBladeDev-plugins/src/loaders/github-docs-loader.ts` to recursively collect `.md`/`.mdx` relative POSIX paths inside `es/docs/<pluginId>` and `en/docs/<pluginId>` (skipping version subdirectories matching `isVersionSlug`), and verify nested slug mismatches (e.g., `api/*.md`) trigger warnings.
- [x] 1.2 Add unit tests in `DarkBladeDev-plugins/src/loaders/github-docs-loader.test.ts` covering nested `api/` extraction, local sync, and recursive `validateDocSlugs` parity checks, and verify `pnpm test` and `pnpm check` pass in `DarkBladeDev-plugins`.

## 2. Author English API Documentation (`docs/en/api/`)

- [x] 2.1 Create `docs/en/api/index.md` (`sidebar.order: 1`) covering `croppy-api` hexagonal boundaries, Gradle setup (`0.10.0`), package map across the 17 API packages, and core identifiers/coordinates (`Key`, `Keyed`, `BlockPosition`, `ChunkPosition`) verified against `croppy-api/src/main/java`.
- [x] 2.2 Create `docs/en/api/addon-development.md` (`sidebar.order: 2`) migrating Java addon development (`CroppyAddon`, `AddonContext`, `PaperAddonContext`, `AddonClassLoader` DAG, 5-phase decommissioning, dormant crops) and documenting `AddonConfig` and `croppy-api` growth hooks (`GrowthModifier`, `GrowthContext`, `CropEnvironment`, `StageAdvanceInterceptor`, `UnwateredPolicy`, `WitherHandler`).
- [x] 2.3 Create `docs/en/api/typed-variables.md` (`sidebar.order: 3`) migrating and expanding `DataType<T>`, `SimpleDataType<T>`, `CompoundDataType<T>`, `DataTypeRegistry` (`getTyped`), `TypedVariableStore`, `TypedVariable<T>`, `VariableOperation<T>`, `OperationArgs`, `VariableStoreBinaryCodec`, and `UnresolvedVariableData`.
- [x] 2.4 Create `docs/en/api/expressions-and-providers.md` (`sidebar.order: 4`) documenting `VariableProvider`, `EvaluationContext`, `Expression`, `VariableResolver`, standard `StationComponent` namespaces (`thermal.*`, `heat.*`, `storage.*`, `process.*`, `display.*`, `var.*`), legacy aliases, Pratt AST evaluation, and `ContextualTextFormatter`.
- [x] 2.5 Create `docs/en/api/ports-and-registries.md` (`sidebar.order: 5`) documenting `Registry<T>`, all 12 domain registries exposed on `AddonContext` plus `DataTypeRegistry`, `CroppyLogger` (`LogLevel`, `LogChannel`, lazy `Supplier<String>`, `.scoped()`), and SPI ports (`ProtectionPort`, `ProtectionAction`, `EconomyPort`, `TranslationProvider`, `MessageKey`, `LocaleRef`, `PaperMessageService#arg`).

## 3. Refactor English Root Guides (`docs/en/`)

- [x] 3.1 Refactor `docs/en/addon-system.md` to retain layering, `addon.yml`, Declarative Content Packs, and `/croppy addon` commands while replacing Java/DAG/5-phase/variable-extension sections with concise cross-links to `./api/addon-development.md` and `./api/typed-variables.md`.
- [x] 3.2 Refactor `docs/en/workstations-configuration.md` and `docs/en/rules-and-interactions.md` to keep YAML schemas (`states:`, `default_variables:`, `interactions/`, `rules/`) and display placeholders, harmonize legacy rule variable examples with v0.10.0 namespaces, and link internal Java/AST/codec details to `./api/typed-variables.md` and `./api/expressions-and-providers.md`.
- [x] 3.3 Update `docs/en/integrations.md`, `docs/en/localization.md`, `docs/en/growth-and-environment.md`, and `docs/en/index.md` to link SPI ports (`ProtectionPort`, `EconomyPort`, `TranslationProvider`), `GrowthModifier`/`WitherHandler`, and `croppy-api` overview cleanly to `./api/*`.

## 4. Mirror & Translate Spanish Documentation (`docs/es/`)

- [x] 4.1 Create all 5 translated pages in `docs/es/api/` (`index.md`, `addon-development.md`, `typed-variables.md`, `expressions-and-providers.md`, `ports-and-registries.md`) matching `docs/en/api/` filenames, `sidebar.order`, code blocks, and verified `croppy-api` symbols.
- [x] 4.2 Refactor `docs/es/addon-system.md`, `docs/es/workstations-configuration.md`, `docs/es/rules-and-interactions.md`, `docs/es/integrations.md`, `docs/es/localization.md`, `docs/es/growth-and-environment.md`, and `docs/es/index.md` to mirror `docs/en/` structure and `./api/*` cross-links.

## 5. Verification

- [x] 5.1 Verify 1:1 relative path parity between `docs/en/` and `docs/es/` (including `api/`), check all relative Markdown links resolve to existing files, and confirm `.\gradlew.bat test` passes without regressions.
- [x] 5.2 Verify `DarkBladeDev-plugins` local sync (`syncLocalDocs` / `pnpm build`) ingests `docs/<lang>/api/*.md` without slug mismatch warnings and renders valid Starlight routes.
