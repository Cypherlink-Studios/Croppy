# Proposal

## Why

As Croppy expands its simulation of agriculture, ingredients, processing stations, and culinary systems, hardcoding all content and mechanics into the main plugin creates tight coupling, slows development, and prevents third-party developers and server owners from easily adding modular packs or complex Java mechanics. 

Introducing an official, decoupled **Addon System** allows developers to inject complex Java logic (custom growth modifiers, interactive stations, custom rules) and content creators to publish declarative packs (crops, items, recipes, models, textures) in self-contained units (`plugins/Croppy/addons/`), with full support for safe hot-swapping, ClassLoader dependency graphs, and automated companion resource pack merging.

## What Changes

- Introduce the `croppy-api` addon lifecycle SPI: `CroppyAddon`, `AddonContext`, `AddonDescription`, and `AddonState`.
- Support two distinct addon formats via a unified `addon.yml` manifest:
  - **Declarative Content Packs**: Pure YAML definitions and companion assets without bytecode.
  - **Java Code Addons**: Modular JARs providing an entrypoint implementing `CroppyAddon`.
- Implement an in-memory **ClassLoader Graph** supporting cross-addon dependencies (`Addon A -> Addon B -> Croppy Core`) with Kahn's topological sort and circular dependency rejection.
- Implement a **5-Phase Decommissioning Protocol** for safe hot-swapping (`disable` and `unload`), guaranteeing zero Metaspace leaks, deterministic Bukkit listener deregistration, asynchronous thread draining, and domain registry purging.
- Add `unregister(Key)` and scoped unregistration support to `Registry<T>` in `croppy-api` and `croppy-core`.
- Automatically scan and merge addon `assets/<namespace>/` trees into the Croppy companion resource pack compiler.
- Register a dedicated administrative command tree `/croppy addon <list|status|load|unload|enable|disable> [addonId]` using Cloud Brigadier.
- Retain global folders (`crops/`, `items/`, `recipes/`, `stations/`) as the high-priority Local Server Override Layer.

## Capabilities

### New Capabilities
- `addon-system`: Defines the addon discovery, manifest format, lifecycle state machine (`ON_DISK`, `LOADED`, `ENABLED`), DAG ClassLoader graph, tracked execution context, and hot-swap safety protocols.

### Modified Capabilities
- `core-registry`: Expands the generic `Registry<T>` contract to support thread-safe entry unregistration (`unregister(Key)`) and bulk namespace removal to support dynamic addon unloading.
- `companion-resource-pack`: Extends asset hierarchy rules to allow multiple addon namespaces (`assets/<namespace>/`) and support automated aggregation of addon textures and models into the generated companion pack.
- `crop-commands`: Adds the `/croppy addon <list|status|load|unload|enable|disable> [addonId]` administrative command tree.

## Impact

- **API & Core**: `croppy-api` adds new addon contracts without adding external dependencies. `croppy-core` implements addon discovery, dependency graph resolution, and registry unregister support.
- **Paper Adapter**: `croppy-paper` provides `AddonClassLoader`, manages Bukkit event listener registration/unregistration, manages executor lifecycles, and integrates addon commands into Cloud Paper.
- **Backwards Compatibility**: Fully backwards compatible. Existing configurations, crops, items, and recipes in `plugins/Croppy/` remain functional and take precedence over addon defaults as the local override layer.
