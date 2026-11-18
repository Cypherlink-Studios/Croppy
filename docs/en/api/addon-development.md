---
title: Java Addon Development
description: Developer guide to CroppyAddon, AddonContext, AddonConfig, ClassLoader DAG resolution, 5-phase hot-swapping, and growth extension hooks.
sidebar:
  order: 2
---

# Java Addon Development

Java code addons extend Croppy with custom gameplay listeners, asynchronous tasks, growth modifiers, typed variable types, and component variable providers—all managed inside isolated `AddonClassLoader` environments with leak-proof hot-swapping.

> For declarative YAML content packs, `addon.yml` manifest format, and `/croppy addon` admin commands, see the [Addon System Guide](../addon-system.md).

---

## 1. ClassLoader Graph & Horizontal Delegation

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
| (e.g. "croppy-culinary-base")|  Horizontal  |  (e.g. "pastry-expansion")  |
+-----------------------------+  Delegation   +-----------------------------+
```

When an addon resolves a class:
1. **Local Lookup**: Checks `findLoadedClass` and searches the addon's own JAR.
2. **Horizontal Delegation**: If not found locally, queries the `AddonClassLoader` of each addon declared under `dependencies:` or `softDependencies:` in `addon.yml`, allowing Addon B to reference exported classes from Addon A.
3. **Parent Fallback**: If absent from all dependencies, delegates to Croppy's parent classloader (`croppy-api`, Paper, Java runtime).

Circular dependencies (e.g., `A -> B -> A`) are detected via Kahn's algorithm during startup and rejected cleanly.

---

## 2. Lifecycle & Safe Hot-Swapping

Addons transition through three `AddonState` values: `ON_DISK` → `LOADED` → `ENABLED`.

### The 5-Phase Decommissioning Protocol

To prevent JVM Metaspace leaks, ghost event listeners, and orphaned worker threads when running `/croppy addon disable` or `/croppy addon unload`, Croppy executes a deterministic 5-phase teardown:

```text
[ Phase 1: Notification ]  ──> Invokes CroppyAddon.onDisable(AddonContext).
         |
         v
[ Phase 2: Unregistration ] ─> Paper: HandlerList.unregisterAll() on tracked listeners
         |                     and cancels all tracked BukkitTasks.
         v
[ Phase 3: Task Draining ] ──> ManagedExecutorService.shutdownNow() with a 2-second timeout.
         |
         v
[ Phase 4: Registry Purge ] ─> Atomic unregistration of species, items, recipes, crafting
         |                     recipes, categories, processes, stations, menus,
         |                     interactions, and growth modifiers tracked by this addon.
         v
[ Phase 5: Severing & GC ] ──> (Unload only) Closes URLClassLoader and clears graph
                               references so GC can reclaim Metaspace memory.
```

### Dormant Crop Safety

If an addon is disabled or unloaded while its custom crops are planted in the world, `GrowthEngine` marks those instances as **dormant** and skips them during growth ticks without throwing exceptions or corrupting chunk data. Re-enabling the addon seamlessly resumes their lifecycle.

---

## 3. Implementing `CroppyAddon` & `AddonContext`

Your `main` class in `addon.yml` must be `public`, provide a no-argument constructor, and implement `dev.darkblade.croppy.api.addon.CroppyAddon`:

```java
package dev.darkblade.croppy.arcane;

import dev.darkblade.croppy.api.addon.AddonConfig;
import dev.darkblade.croppy.api.addon.AddonContext;
import dev.darkblade.croppy.api.addon.CroppyAddon;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.platform.paper.addon.PaperAddonContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArcaneFarmingAddon implements CroppyAddon, Listener {

    @Override
    public void onEnable(AddonContext context) {
        // Extract bundled config.yml if absent and read typed settings
        context.saveDefaultConfig();
        AddonConfig config = context.getConfig();
        double bonusMultiplier = config.getDouble("moonlight.multiplier", 1.75);

        context.getCroppyLogger().info(LogChannel.ADDON,
                "Arcane Farming v" + context.getDescription().version() + " initialized!");

        // Register a growth modifier (e.g. boost growth in low light with high moisture)
        GrowthModifier moonlightModifier = ctx ->
                (ctx.environment().lightLevel() <= 7 && ctx.environment().waterFluidLevel() >= 4)
                        ? bonusMultiplier
                        : 1.0;

        // Track Bukkit listeners & domain hooks via PaperAddonContext for safe hot-swapping
        context.getPlatformAdapter(PaperAddonContext.class).ifPresent(paperCtx -> {
            paperCtx.trackGrowthModifier(moonlightModifier);
            paperCtx.registerListener(this);
        });

        // Managed background work tied to addon teardown
        context.runAsync(() ->
                context.getCroppyLogger().debug(LogChannel.ADDON, () -> "Async arcane setup complete."));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Automatically unregistered on disable/unload
    }

    @Override
    public void onDisable(AddonContext context) {
        context.getCroppyLogger().info(LogChannel.ADDON, "Arcane Farming gracefully disabled.");
    }
}
```

### `AddonContext` Capabilities

* **Metadata & Files**: `getDescription()` (`AddonDescription`), `getDataFolder()` (`Path`).
* **Logging**: `getLogger()` (bridged JUL `Logger`) and `getCroppyLogger()` (`CroppyLogger` scoped to `Addon:<id>`).
* **Configuration**: `getConfig()`, `saveDefaultConfig()`, `reloadConfig()`, `saveConfig()`.
* **Concurrency**: `runAsync(Runnable)` submits tasks to a daemon executor drained automatically on disable.
* **Domain Registries**: 12 registry accessors (see [Ports & Registries](./ports-and-registries.md#2-domain-registries-via-addoncontext)).
* **Platform Adapter**: `getPlatformAdapter(PaperAddonContext.class)` exposes `registerListener(Listener)`, `trackTask(BukkitTask)`, `trackGrowthModifier(GrowthModifier)`, and `track*` helpers for manual key registrations.

---

## 4. Addon Configuration (`AddonConfig`)

`dev.darkblade.croppy.api.addon.AddonConfig` provides a pure Java 21 YAML configuration abstraction backed by Bukkit's comment-preserving configuration on Paper (with child-first `config.yml` resource isolation):

| Method | Return Type | Behavior |
|---|---|---|
| `getString(path)` / `getString(path, def)` | `String` | Returns `null` or `def` when absent. |
| `getInt(path)` / `getInt(path, def)` | `int` | Defaults to `0` or `def`. |
| `getLong(path)` / `getLong(path, def)` | `long` | Defaults to `0L` or `def`. |
| `getDouble(path)` / `getDouble(path, def)` | `double` | Defaults to `0.0` or `def`. |
| `getBoolean(path)` / `getBoolean(path, def)` | `boolean` | Defaults to `false` or `def`. |
| `getStringList(path)` | `List<String>` | Returns an unmodifiable list (empty list if absent). |
| `contains(path)` | `boolean` | Checks whether the dot-separated key exists. |
| `getKeys(deep)` | `Set<String>` | Lists shallow or recursive keys in the current section. |
| `getSection(path)` | `Optional<AddonConfig>` | Navigates to a nested configuration subsection. |
| `set(path, value)` | `void` | Mutates an in-memory value (`null` removes the key). |
| `reload()` / `save()` | `void` | Reloads from or persists to disk. |

---

## 5. Agricultural Growth Hooks (`dev.darkblade.croppy.api.growth`)

Pure domain contracts let addons inspect and influence crop simulation (see [Growth & Environment](../growth-and-environment.md)):

* **`GrowthModifier`**: `@FunctionalInterface` computing `double calculateModifier(GrowthContext context)` (`1.0` = normal, `> 1.0` = accelerated, `< 1.0` = slowed, `0.0` = halted).
* **`GrowthContext(CropInstance crop, Species species, CropEnvironment environment, double baseProgress)`**: Immutable snapshot passed to every modifier.
* **`CropEnvironment(int lightLevel, int waterFluidLevel, boolean isLoaded)`**: Ambient block light (`0–15`), nearby water fluid level (`0–8`), and chunk load flag.
* **`StageAdvanceInterceptor`**: `@FunctionalInterface` (`shouldAdvance(crop, species, fromStage, toStage)`) to approve or veto stage transitions.
* **`UnwateredPolicy`**: Immutable record with factory methods `pause()`, `slow(multiplier)`, `decay(amount)`, and `wither(maxDryDays)`.
* **`WitherHandler`**: `@FunctionalInterface` (`onWither(crop, species)`) invoked when a crop exceeds its dry-day threshold.

---

## 6. Addon Extension Ports (`DatabaseService`, `ProcessListener`, `ItemMetadataContributor`)

`AddonContext` provides direct access to platform-independent persistence, process lifecycle hooks, and item metadata round-tripping—automatically scoped and cleaned up on addon disable:

* **Scoped SQLite Pool (`getDatabaseService()`)**: Returns a `DatabaseService` scoped to the addon's sanitized table prefix (`addon_<id>_`). Use `table("my_table")` (validated against `[a-zA-Z0-9_]+`) with `execute(...)`, `query(...)`, or `getConnection()` to share Croppy's HikariCP pool without table collisions.
* **Process Lifecycle Hooks (`registerProcessListener(ProcessListener)`)**: Subscribes to `ProcessEngine` callbacks (`onProcessStart`, `onProcessTick`, `onPhaseAdvance`, `onProcessComplete`, `onProcessFail`). Exceptions inside listeners are isolated and tracked listeners are automatically unregistered on addon disable.
* **Item Metadata & Quality Round-Trip (`getItemMetadataRegistry()`, `ItemMetadataContributor`)**: Registers contributors that serialize `StoredItem.properties()` into namespaced PDC tags (`serializeMetadata`), append MiniMessage lore (`formatLore`), and restore domain properties in `ItemFactory.toStoredItem(ItemStack)` (`deserializeMetadata`).
* **Typed Variables & Discovery (`getDataTypeRegistry()`, `getRecipeDiscoveryEngine()`)**: Exposes custom `VariableDataType` registration and programmatic recipe discovery queries/grants.
