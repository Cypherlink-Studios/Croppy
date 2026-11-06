# Design

## Context

Croppy is structured as a hexagonal architecture across three modules (`croppy-api`, `croppy-core`, and `croppy-paper`). All domain entities (`Species`, `ItemDefinition`, `RecipeDefinition`, `StationDefinition`) are keyed by namespaced identifiers (`Key`) and registered in in-memory registries implementing `Registry<T>`.

Currently, all content is loaded exclusively from hardcoded subdirectories in the plugin data folder (`crops/`, `items/`, etc.). To enable third-party ecosystem extensibility without degrading server stability or performance, we design an Addon Engine capable of handling both declarative content packs and Java code addons with strict isolation, dependency graph resolution, and deterministic hot-swap lifecycle management.

See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Provide a unified addon format (`addon.yml`) supporting both declarative packs (YAML + textures) and Java bytecode addons.
- Build a DAG ClassLoader graph that enables cross-addon dependency resolution while rejecting cyclic references.
- Implement a 5-phase decommissioning protocol ensuring zero Metaspace leaks, deterministic listener cleanup, and thread executor termination during `/croppy addon disable/unload`.
- Preserve the existing global directories as the high-priority Local Server Override layer.
- Integrate addon assets automatically into the companion resource pack compiler.

**Non-Goals:**
- Replacing Paper's plugin system for entirely unrelated plugins (addons are strictly designed to extend Croppy mechanics).
- Supporting legacy Spigot or CraftBukkit versions below Paper 1.21.8+.
- Sandboxing bytecode execution with a Java SecurityManager (deprecated in modern Java).

## Architecture Overview

```
+-----------------------------------------------------------------------------------------+
|                                    CROPPY ADDON ENGINE                                  |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|   /croppy addon <list|status|load|unload|enable|disable>                                |
|            |                                                                            |
|            v                                                                            |
|   [ PaperAddonManager ] (Orchestrates discovery, states, and resource pack compiler)    |
|            |                                                                            |
|            +---> [ AddonGraphValidator ] (Kahn's DAG topological sorter & cycle checker)|
|            +---> [ AddonClassLoader Graph ] (Horizontal class delegation across addons)  |
|            +---> [ Tracked AddonContext ] (Scoped listeners, executors, registries)     |
|            +---> [ AddonDecommissioner ] (5-phase leak-proof cleanup protocol)          |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

## Decisions

### 1. Unified `addon.yml` Manifest with Optional `main`
- **Decision**: Use a single manifest schema for both declarative packs and Java code addons. If `main` is omitted, the engine treats the addon as a declarative content pack. If `main` is present, it validates that the target class implements `CroppyAddon`.
- **Alternatives Considered**: Separate folders (`addons/` for code, `packs/` for data). Rejected because hybrid addons (combining custom Java mechanics with custom items/models) are common and benefit from single-package distribution.

### 2. ClassLoader Graph with Horizontal Delegation
- **Decision**: Implement `AddonClassLoader extends URLClassLoader`. When `findClass(name)` is invoked:
  1. Check local loaded classes (`findLoadedClass`).
  2. Search the addon's own JAR.
  3. If not found locally, iterate through declared dependencies and query their `AddonClassLoader` via a non-recursive `findClassLocally(name)`.
  4. Fallback to parent ClassLoader (`CroppyPlugin` / Paper).
- **Alternatives Considered**: Flat single ClassLoader for all addons. Rejected because independent addons would share a classpath, causing class name collisions and preventing independent hot-unloading.

### 3. Five-Phase Decommissioning Protocol for Safe Hot-Swapping
- **Decision**: Disabling and unloading an addon executes five discrete phases:
  1. **Notification**: Call `addon.onDisable(context)` inside a fault-tolerant try-catch block.
  2. **Listener Deregistration**: Bukkit `HandlerList.unregisterAll()` on every listener tracked in `PaperAddonContext`.
  3. **Task & Thread Draining**: Cancel tracked `BukkitTask` instances and invoke `shutdownNow()` with a 2-second timeout on the addon's `ManagedExecutorService`.
  4. **Registry Purging**: Call `Registry.unregister(key)` across all core registries for keys registered by this addon. Mark crops in the world dormant.
  5. **ClassLoader Severing**: Close `URLClassLoader`, clear graph references, and nullify internal state.
- **Alternatives Considered**: Reloading the entire Paper plugin via PluginManager. Rejected because server-wide plugin reloads are notorious for memory leaks and disrupt unrelated systems.

### 4. Registry Unregistration Support in Core
- **Decision**: Extend `Registry<T>` in `croppy-api` with `boolean unregister(Key key)` and `int unregisterAll(Predicate<T> filter)`, implemented in `SimpleRegistry`.
- **Alternatives Considered**: Rebuilding registries from scratch on each addon change. Rejected because it incurs unnecessary I/O overhead and invalidates running caches.

## Risks / Trade-offs

- **[Risk] World Crop Instances Orphaned during Addon Unload** → *Mitigation*: When an addon's species is unregistered, world crop instances are flagged as `DORMANT`. Growth engine skips dormant crops without throwing exceptions, and visual entities remain display-safe.
- **[Risk] Hanging Threads from Third-Party Addon Code** → *Mitigation*: `PaperAddonContext` requires asynchronous tasks to run through its managed executor service. Raw unmanaged threads are flagged during development guidelines, and executor `shutdownNow()` interrupts worker threads.
- **[Risk] Asset Collation Conflicts in Resource Pack** → *Mitigation*: Addons must place assets inside `assets/<namespace>/` matching their declared namespace. Assets in `assets/croppy/` from third-party addons are rejected during validation.

## Migration & Compatibility Plan

- Existing server installations keep their existing configuration and definitions in `plugins/Croppy/` untouched.
- The `plugins/Croppy/addons/` directory is created automatically on plugin startup if absent.
- The Local Server Layer (`crops/`, `items/`, `recipes/`, `stations/`) is scanned after enabled addons, ensuring server owners can override any addon definition without modifying addon JARs.
