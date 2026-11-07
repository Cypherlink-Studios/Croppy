# Proposal: Addon Configuration System

## Why

Croppy's modular addon engine currently supports declarative content packs and Java code addons with ClassLoader graphs, but lacks a standard configuration and settings persistence system. Addons currently have no standardized way to read custom settings, save default configurations (`config.yml`), reload configs dynamically, or guard against path collisions when both JAR and directory addons reside in `plugins/Croppy/addons/`. Introducing an ergonomic, decoupled `AddonConfig` subsystem resolves this limitation.

## What Changes

- **Pure API Configuration Contract (`croppy-api`)**: Define `AddonConfig` with type-safe getters (`getString`, `getInt`, `getBoolean`, `getDouble`, `getStringList`, `getSection`), modification methods (`set`), and persistence hooks (`reload`, `save`). Extend `AddonContext` with Bukkit-familiar lifecycle methods (`getConfig()`, `saveDefaultConfig()`, `reloadConfig()`, `saveConfig()`).
- **Decoupled Core Configuration Engine (`croppy-core`)**: Provide `YamlAddonConfig` using SnakeYAML 2.2, enabling 100% headless unit testing and offline manipulation without Paper or Bukkit dependencies.
- **Paper Platform Configuration Adapter (`croppy-paper`)**: Implement `PaperAddonConfig` adapting Bukkit's `YamlConfiguration` to preserve inline comments, header comments, and formatting during runtime on Paper servers.
- **Data Folder Binding Rules**:
  - For directory addons, the addon root directory is directly treated as its `dataFolder`.
  - For compiled JAR addons, `dataFolder` resolves to `plugins/Croppy/addons/<addon-id>/`.
- **Path Ownership Gate & Collision Prevention**: Implement defensive path ownership validation in `DefaultAddonManager` and `PaperAddonManager` to fail-fast with descriptive errors upon detecting duplicate addon IDs or cross-occupancy collisions (e.g. a JAR addon attempting to use a data folder already occupied by a different directory addon).

## Capabilities

### New Capabilities
<!-- None: Configuration and path collision management expand the existing addon architecture. -->

### Modified Capabilities
- `addon-system`: Add requirements for Addon Configuration Management (`AddonConfig` contract, default resource extraction, reloading) and Path Ownership Collision Protection.

## Impact

- **API (`croppy-api`)**: New interface `AddonConfig`; new methods on `AddonContext`. No breaking changes to existing methods.
- **Core (`croppy-core`)**: New class `YamlAddonConfig` using existing SnakeYAML dependency; updated `DefaultAddonManager` with path collision detection.
- **Platform (`croppy-paper`)**: New class `PaperAddonConfig`; updated `PaperAddonContext` to provide config instances; updated `PaperAddonManager` with extraction logic and path ownership validation.
- **Addons (`Croppy-addons`)**: Addons like `croppy-coffee` can author `config.yml` and consume `context.getConfig()` and `context.saveDefaultConfig()`.
