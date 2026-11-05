# Proposal: Cleanup Deprecated Config Sections

## Why

During early development (v0.1.0–v0.2.0), `config.yml` served as a catch-all configuration file housing inspection tool item definitions (`inspection-tool:`) and flat message strings (`messages:`). With subsequent architectural migrations to the unified `ItemRegistry` (`items/tools/inspector.yml`) and the full internationalization system (`PaperMessageService` / `locales/*.yml`), these sections became legacy dead code. Maintaining them in `config.yml` causes administrator confusion, as editing `messages.*` or `inspection-tool.*` produces no observable change in game behavior, while cluttering the configuration file with unused fields.

## What Changes

- **Purge Deprecated `inspection-tool` Section**: Remove `inspection-tool.material`, `inspection-tool.name`, and `inspection-tool.lore` from `config.yml`. The official inspection tool is already managed via `items/tools/inspector.yml` (`croppy:inspection_tool`).
- **Purge Deprecated `messages` String Section**: Remove legacy MiniMessage template strings (`prefix`, `no-permission`, `no-crop-found`, `tool-given`, `reload-success`, `reload-failed`, `stage-updated`, `progress-updated`, `give-success`, `player-not-found`, `species-not-found`) from `config.yml`. All messages are loaded dynamically from `locales/en-US.yml` and `locales/es-ES.yml`.
- **Relocate Locale Configuration to `locale.default`**: Relocate the default server locale from `messages.default-locale` to `locale.default: "en-US"`, retaining transparent backward compatibility by checking `messages.default-locale` if `locale.default` is omitted.
- **Refactor `PluginConfig`**: Convert `PluginConfig` into a clean data holder or refactor it away from dead item and message formatting methods, eliminating uncalled methods like `CropInspectorService.createInspectionTool()`.
- **Update Documentation**: Synchronize `docs/en/configuration.md` and `docs/es/configuration.md` to reflect the streamlined `config.yml` structure and redirect admins to `items/` and `locales/`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `crop-commands`: Clarify configuration hot reload and confirm that the inspection tool is exclusively sourced and customized via `ItemRegistry` definitions, removing legacy references to `config.yml` tool properties.
- `localization-system`: Update default locale resolution to read from `locale.default` in `config.yml` while providing backward compatibility fallback to `messages.default-locale`.

## Impact

- **Configuration**: `config.yml` drops from 61 lines to ~35 clean, focused lines (`growth`, `storage`, `locale`).
- **Runtime Code**: `croppy-paper` removes dead helper methods in `PluginConfig` and `CropInspectorService.createInspectionTool()`.
- **Backward Compatibility**: Fully preserved for existing deployments; if a server's `config.yml` still specifies `messages.default-locale`, the plugin seamlessly resolves it.
- **Documentation**: English and Spanish documentation updated to accurately describe the cleaned `config.yml`.
