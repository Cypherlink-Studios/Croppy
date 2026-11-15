---
title: Addon System & Extensibility
description: Complete architectural and practical guide to Croppy's decoupled Addon System, supporting declarative content packs, Java bytecode expansions, DAG ClassLoader graphs, and safe hot-swapping.
sidebar:
  order: 21
---

# Addon System & Extensibility

Croppy features an official, decoupled **Addon Engine** that enables both server administrators and Java developers to extend the plugin's simulation, crops, items, culinary stations, and recipes without modifying the core codebase.

The system is designed with strict boundaries:
- **Declarative Content Packs**: Pure YAML definitions and resource pack assets without requiring Java compilation.
- **Java Code Addons**: Modular JARs providing a `CroppyAddon` entrypoint inside isolated `AddonClassLoader` environments with DAG dependency delegation and safe 5-phase hot-swapping — see [Java Addon Development](./api/addon-development.md).
- **Extensible Variables & Contracts**: Addons can register custom `DataType<T>` persistence schemas, `VariableProvider` namespaces, and `GrowthModifier` hooks — see the [API Reference](./api/index.md).
- **Automated Resource Pack Merging**: Addon assets located under `assets/<namespace>/` are automatically aggregated into the server's companion resource pack.

---

## 1. Architectural Model & Layering

Croppy implements a three-tier content layering model:

```text
+-------------------------------------------------------------------------------+
|                       CONTENT & LIFECYCLE HIERARCHY                           |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [ 1. LOCAL SERVER OVERRIDES ] (plugins/Croppy/crops/, items/, etc.)          |
|     * Highest Priority: Overrides any addon or core definition.               |
|     * Fast server-admin tweaks without packaging or recompiling.              |
|                                                                               |
|  [ 2. ADDON LAYER ] (plugins/Croppy/addons/*.jar or folders)                  |
|     * Distributable units with their own namespace and addon.yml manifest.    |
|     * May contain declarative YAMLs, custom 3D models/textures, or Java code. |
|                                                                               |
|  [ 3. CROPPY DEFAULT CORE ] (Internal factory defaults)                       |
|     * Base agricultural and culinary assets shipped with Croppy.              |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. The Addon Manifest (`addon.yml`)

Every addon must declare an `addon.yml` manifest at its root:

```yaml
# Unique addon identifier (lowercase alphanumeric, dashes, dots, underscores)
id: arcane-farming

# Human-readable display name
name: "Arcane Farming"

# Semantic version
version: "1.0.0"

# Primary resource namespace owned by this addon
namespace: arcane

# Optional Java entrypoint implementing CroppyAddon.
# If omitted or left empty, Croppy treats this as a Declarative Content Pack!
main: dev.darkblade.croppy.arcane.ArcaneFarmingAddon

# Optional author metadata
authors:
  - DarkBladeDev
  - ContributorOne

# Optional brief description
description: "Magical crops, moon phases, and altar infusion mechanics."

# Optional list of required addon dependencies (topologically resolved)
dependencies:
  - croppy

# Optional list of soft dependencies (loaded before this addon if present)
softDependencies:
  - exotic-fruits
```

### Validation Constraints
* `id` and `namespace`: Must strictly match regex `^[a-z0-9_.-]+$`.
* `main`: Must point to a public class with a no-arg constructor implementing `dev.darkblade.croppy.api.addon.CroppyAddon`.
* Circular dependencies (e.g. `A -> B -> A`) are detected via Kahn's algorithm and rejected during startup.

---

## 3. Java Developer API

Java code addons interact with Croppy exclusively through `croppy-api` contracts (with optional Paper platform bridging via `PaperAddonContext`). All technical specifications—classloader graph delegation, the 5-phase decommissioning protocol, typed custom variables, expression providers, and SPI ports—now live in the dedicated API Reference:

- **[API Overview & Core Contracts](./api/index.md)**: Hexagonal architecture, Gradle setup, package map, and `Key` / `BlockPosition` value types.
- **[Java Addon Development](./api/addon-development.md)**: `CroppyAddon`, `AddonContext`, `PaperAddonContext`, `AddonConfig`, `AddonClassLoader` DAG delegation, 5-phase hot-swapping, dormant crop safety, and growth hooks.
- **[Typed Custom Variables](./api/typed-variables.md)**: Custom `DataType<T>` schemas, `DataTypeRegistry`, `TypedVariableStore`, `VariableOperation<T>`, and `VariableStoreBinaryCodec`.
- **[Expressions & Variable Providers](./api/expressions-and-providers.md)**: Component `VariableProvider` namespaces, `EvaluationContext`, Pratt AST compilation, and `ContextualTextFormatter`.
- **[Ports, Registries & Logging](./api/ports-and-registries.md)**: `Registry<T>`, domain registries, `CroppyLogger`, `ProtectionPort`, `EconomyPort`, and `TranslationProvider`.

---

## 4. Content Creator Guide: Declarative Packs

A Declarative Content Pack requires zero Java knowledge. Simply place your files in `plugins/Croppy/addons/<your-pack>/`:

```text
plugins/Croppy/addons/exotic-fruits/
├── addon.yml
├── crops/
│   └── dragonfruit.yml
├── items/
│   ├── seeds/
│   │   └── dragonfruit_seeds.yml
│   └── ingredients/
│       └── dragonfruit.yml
├── recipes/
│   └── dragonfruit_smoothie.yml
└── assets/
    └── exotic_fruits/
        ├── models/item/dragonfruit.json
        └── textures/item/dragonfruit.png
```

Croppy will automatically:
1. Parse `addon.yml`.
2. Load all crop species, items, and culinary recipes into core registries.
3. Collate `assets/exotic_fruits/` into the server's companion resource pack.

---

## 5. Command Reference

All addon management commands require the `croppy.admin.addon` permission:

| Command | Description |
|---|---|
| `/croppy addon list` | Lists all discovered addons, their version, type (Java / Pack), and state (`ON_DISK`, `LOADED`, `ENABLED`). |
| `/croppy addon status <addonId>` | Displays detailed metadata, loaded classes, registered species/items, active listeners, and task metrics. |
| `/croppy addon load <addonId \| all>` | Loads manifest and initializes ClassLoader into memory (`LOADED` state). |
| `/croppy addon enable <addonId \| all>` | Activates the addon, invokes `onEnable`, and registers its domain content (`ENABLED` state). |
| `/croppy addon disable <addonId \| all>` | Executes the 5-phase decommissioning protocol, unregistering listeners and registries (`LOADED` state). |
| `/croppy addon unload <addonId \| all>` | Completely closes ClassLoaders and detaches memory references (`ON_DISK` state). |
