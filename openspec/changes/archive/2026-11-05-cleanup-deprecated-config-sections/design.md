# Design: Cleanup Deprecated Config Sections

## Context

During initial development (v0.1.0), `config.yml` served as a flat configuration file containing tool customization (`inspection-tool:`) and in-game chat messages (`messages:`).
As Croppy matured:
1. `ItemRegistry` and `ItemFactory` (`items/tools/inspector.yml`) took over custom item definitions (`croppy:inspection_tool`), rendering `inspection-tool.material`, `name`, and `lore` dead code.
2. `PaperMessageService` and `locales/*.yml` (`localization-system`) took over all player-facing and administrative text strings, leaving `messages.*` dead code (except for `default-locale`).

See `proposal.md` for full motivation.

## Goals / Non-Goals

**Goals:**
- Streamline `croppy-paper/src/main/resources/config.yml` to contain only active configuration domains: `growth`, `storage`, and `locale`.
- Introduce `locale.default` for setting the server fallback locale, maintaining backward compatibility with existing `messages.default-locale` definitions.
- Refactor `PluginConfig.java` to act as a typed, structured configuration container for active settings (`growth`, `storage`, `locale`) while purging dead MiniMessage parsing and tool formatting methods.
- Remove dead `CropInspectorService.createInspectionTool()` method.
- Update `docs/en/configuration.md` and `docs/es/configuration.md` to document the modernized configuration file and link to `items/` and `locales/`.

**Non-Goals:**
- Modifying `ItemRegistry`, `ItemFactory`, or `items/tools/inspector.yml`.
- Modifying `PaperMessageService`, `DefaultMessageCatalog`, or YAML locale loaders.
- Adding new simulation or storage configuration options outside this scope.

## Decisions

### Decision 1: Relocate default locale to `locale.default` with fallback
- **Choice**: Read `locale.default` with fallback to `messages.default-locale` and default `"en-US"`:
  ```java
  String defaultLocaleStr = config.getString("locale.default",
          config.getString("messages.default-locale", "en-US"));
  ```
- **Rationale**: Groups localization settings cleanly without keeping an empty or deceptive `messages:` root section. Existing server installations do not break.
- **Alternatives Considered**:
  - Keep `messages.default-locale`: Misleading because server owners expect message templates to live in `messages:`.

### Decision 2: Refactor `PluginConfig` into a typed settings holder
- **Choice**: Refactor `PluginConfig.java` to store and provide typed accessors:
  - `growthTickInterval` (long, default 20L)
  - `growthBaseProgress` (double, default 1.0)
  - `growthWaterSearchRadius` (int, default 4)
  - `storageType` (String, default "pdc")
  - `defaultLocale` (String, default "en-US")
  Purge dead methods: `getToolMaterial()`, `getToolName()`, `getToolLore()`, `getMessage()`, `getMessageWithoutPrefix()`.
- **Rationale**: Centralizes config parsing instead of calling `getConfig().getLong(...)` in multiple places in `CroppyPlugin.java`. Eliminates dead message formatting code while preserving constructor compatibility across tests.
- **Alternatives Considered**:
  - Delete `PluginConfig` entirely: Would break several class signatures (`CroppyCommands`, `InspectionToolListener`) and associated test suites.

### Decision 3: Remove `CropInspectorService.createInspectionTool()`
- **Choice**: Eliminate the static helper method `createInspectionTool(Plugin, PluginConfig)`.
- **Rationale**: The official mechanism to create items in Croppy is `ItemFactory.createItemStack(ItemDefinition, int)`. The static helper was never called outside test harnesses or legacy scaffolding.

## Risks / Trade-offs

- **[Risk] Existing servers have `messages.*` in their customized `config.yml`** → **Mitigation**: Those keys were already dead code since v0.3.0. Cleaning `config.yml` ensures that server admins consult the actual source of truth (`plugins/Croppy/locales/`).
- **[Risk] Existing servers omit `locale.default`** → **Mitigation**: Dual fallback ensures `messages.default-locale` is evaluated before falling back to `"en-US"`.

## Migration Plan

1. Update `config.yml` default resource in `croppy-paper`.
2. Refactor `PluginConfig.java` and remove dead code from `CropInspectorService.java` and `InspectionToolListener.java`.
3. Update `CroppyPlugin.java` to use typed configuration values from `PluginConfig`.
4. Update unit and integration tests.
5. Synchronize user documentation in English and Spanish.
