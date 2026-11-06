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
- **Java Code Addons**: Modular JARs providing a `CroppyAddon` entrypoint, executed inside isolated `AddonClassLoader` environments.
- **ClassLoader Graph (DAG)**: Deterministic topological dependency resolution supporting cross-addon class delegation (`Addon A -> Addon B -> Croppy Core`).
- **5-Phase Decommissioning Protocol**: Guaranteed leak-proof hot-swapping (`/croppy addon disable` and `/croppy addon unload`) that cleans up Bukkit event listeners, drains background threads, and purges domain registries.
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

## 3. ClassLoader Graph & Horizontal Delegation

Java code addons are loaded using `AddonClassLoader`, a specialized `URLClassLoader`:

```text
                        +----------------------------+
                        |  Paper Server ClassLoader  |
                        +----------------------------+
                                      |
                                      v
                        +----------------------------+
                        |  CroppyPlugin ClassLoader  |
                        |   (croppy-api, core, etc.) |
                        +----------------------------+
                                      | (Parent)
               +----------------------+----------------------+
               |                                             |
               v                                             v
+-----------------------------+               +-----------------------------+
|    AddonClassLoader [A]     | ------------> |    AddonClassLoader [B]     |
| (ej: "croppy-culinary-base")|  Horizontal   |  (ej: "pastry-expansion")   |
+-----------------------------+  Delegation   +-----------------------------+
```

When an addon attempts to load a class:
1. **Local Lookup**: Checks `findLoadedClass` and searches the addon's own JAR.
2. **Horizontal Delegation**: If not found locally, it queries the `AddonClassLoader` of each addon declared under `dependencies:` or `softDependencies:`, allowing Addon B to safely access exported types from Addon A.
3. **Parent Fallback**: If absent from all dependencies, it delegates to Croppy's parent classloader (`croppy-api`, Paper, Java runtime).

---

## 4. Safe Hot-Swapping: The 5-Phase Protocol

To prevent JVM Metaspace leaks, ghost event listeners, and thread abandonment during runtime reload or unload, Croppy executes a strict 5-phase decommissioning protocol:

```text
[ Phase 1: Notification ] ──> Invokes CroppyAddon.onDisable(AddonContext).
         |
         v
[ Phase 2: Unregistration] ──> Bukkit: HandlerList.unregisterAll() on all tracked listeners.
         |                    Cancels all tracked BukkitTasks.
         v
[ Phase 3: Task Draining ] ──> ManagedExecutorService.shutdownNow() with a 2-second timeout.
         |
         v
[ Phase 4: Registry Purge] ──> Atomic unregistration of species, items, recipes, processes,
         |                    stations, and growth modifiers registered by this addon.
         v
[ Phase 5: Severing & GC ] ──> (Unload only) Closes URLClassLoader and clears graph references,
                              allowing the Garbage Collector to reclaim Metaspace memory.
```

### Dormant Crop Safety
If an addon is disabled or unloaded while its custom crops are planted in the Minecraft world, Croppy's simulation engine (`GrowthEngine`) automatically flags those crop instances as **dormant**. Growth ticks skip dormant crops without throwing `NullPointerException` or corrupting chunks. Once the addon is re-enabled, the crops seamlessly resume their growth cycles.

---

## 5. Developer Guide: Creating a Java Addon

### Step 1: Project Setup (Gradle)
Add `croppy-api` as a compile-only dependency:

```kotlin
dependencies {
    compileOnly("dev.darkblade.croppy:croppy-api:0.5.7")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}
```

### Step 2: Implement `CroppyAddon`
Create your main addon class:

```java
package dev.darkblade.croppy.arcane;

import dev.darkblade.croppy.api.addon.AddonContext;
import dev.darkblade.croppy.api.addon.CroppyAddon;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.platform.paper.addon.PaperAddonContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArcaneFarmingAddon implements CroppyAddon, Listener {

    @Override
    public void onEnable(AddonContext context) {
        context.getLogger().info("Arcane Farming initialized!");

        // 1. Injected custom growth modifier: Crops accelerate at night
        GrowthModifier moonlightModifier = ctx -> {
            boolean isNight = ctx.environment().time() >= 13000 && ctx.environment().time() <= 23000;
            return isNight ? 2.0 : 1.0;
        };

        // 2. Register Bukkit listener via Paper adapter
        context.getPlatformAdapter(PaperAddonContext.class).ifPresent(paperCtx -> {
            paperCtx.trackGrowthModifier(moonlightModifier);
            paperCtx.registerListener(this);
        });

        // 3. Managed asynchronous background work
        context.runAsync(() -> {
            context.getLogger().info("Executing async arcane ritual calculation...");
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Safe, tracked event handling
    }

    @Override
    public void onDisable(AddonContext context) {
        context.getLogger().info("Arcane Farming gracefully disabled.");
    }
}
```

---

## 6. Content Creator Guide: Declarative Packs

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

## 7. Command Reference

All addon management commands require the `croppy.admin.addon` permission:

| Command | Description |
|---|---|
| `/croppy addon list` | Lists all discovered addons, their version, type (Java / Pack), and state (`ON_DISK`, `LOADED`, `ENABLED`). |
| `/croppy addon status <addonId>` | Displays detailed metadata, loaded classes, registered species/items, active listeners, and task metrics. |
| `/croppy addon load <addonId \| all>` | Loads manifest and initializes ClassLoader into memory (`LOADED` state). |
| `/croppy addon enable <addonId \| all>` | Activates the addon, invokes `onEnable`, and registers its domain content (`ENABLED` state). |
| `/croppy addon disable <addonId \| all>` | Executes the 5-phase decommissioning protocol, unregistering listeners and registries (`LOADED` state). |
| `/croppy addon unload <addonId \| all>` | Completely closes ClassLoaders and detaches memory references (`ON_DISK` state). |
