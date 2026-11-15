---
title: Developer API Overview
description: Introduction to croppy-api, hexagonal boundaries, Gradle setup, package layout, and core domain value types.
sidebar:
  order: 1
---

# Croppy Developer API (`croppy-api`)

The **`croppy-api`** module provides pure **Java 21** domain contracts, immutable DTOs, SPI ports, and registry interfaces for extending Croppy without coupling to internal engine implementations (`croppy-core`) or server-specific internals (`croppy-paper`).

---

## 1. Hexagonal Architecture & Module Boundaries

```text
+-----------------------------------------------------------------------+
|                              croppy-api                               |
|   Zero external dependencies (Standard Java 21 Library ONLY)          |
|   * Contracts: CroppyAddon, AddonContext, AddonConfig, VariableProvider|
|   * Types: DataType<T>, TypedVariableStore, Key, BlockPosition        |
|   * Ports: ProtectionPort, EconomyPort, TranslationProvider, Logger   |
+-----------------------------------+-----------------------------------+
                                    | implemented by
         +--------------------------+--------------------------+
         v                                                     v
+-----------------------------------+     +-----------------------------+
|            croppy-core            |     |       External Addons       |
|   Pure domain engine & codecs     |     |   Java JARs & Extensions    |
+-----------------------------------+     +-----------------------------+
                                    | wired by
                                    v
+-----------------------------------------------------------------------+
|                             croppy-paper                              |
|   Paper 1.21.8+ adapter, PacketEvents displays, Bukkit events, SQLite |
+-----------------------------------------------------------------------+
```

* **`croppy-api` contains zero Bukkit, Paper, Kyori Adventure, or YAML dependencies.** Addons can depend on `croppy-api` cleanly for pure unit testing and add `paper-api` only when hooking into Bukkit events or platform adapters.
* Platform-specific helpers (such as registering Bukkit `Listener` instances or tracking `BukkitTask`s) are accessed explicitly via `AddonContext.getPlatformAdapter(PaperAddonContext.class)`.

---

## 2. Gradle Dependency Setup

Target Java **21** and declare `croppy-api` (plus `paper-api` if your addon uses Bukkit/Paper events) as `compileOnly` dependencies:

```kotlin
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly("dev.darkblade.croppy:croppy-api:0.10.0")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}
```

---

## 3. Package Map (`dev.darkblade.croppy.api`)

| Package | Primary Contracts & Records | Documentation |
|---|---|---|
| `addon` | `CroppyAddon`, `AddonContext`, `AddonConfig`, `AddonDescription`, `AddonManager`, `AddonState` | [Addon Development](./addon-development.md) |
| `growth` | `GrowthModifier`, `GrowthContext`, `CropEnvironment`, `StageAdvanceInterceptor`, `UnwateredPolicy`, `WitherHandler` | [Addon Development](./addon-development.md) |
| `variable` | `DataType<T>`, `SimpleDataType<T>`, `CompoundDataType<T>`, `DataTypeRegistry`, `TypedVariableStore`, `TypedVariable<T>`, `VariableOperation<T>`, `OperationArgs`, `UnresolvedVariableData` | [Typed Variables](./typed-variables.md) |
| `expression` | `VariableProvider`, `EvaluationContext`, `Expression`, `VariableResolver` | [Expressions & Providers](./expressions-and-providers.md) |
| `registry` | `Registry<T extends Keyed>`, `Service` | [Ports & Registries](./ports-and-registries.md) |
| `logging` | `CroppyLogger`, `LogLevel`, `LogChannel`, `JulCroppyLogger`, `NoOpCroppyLogger` | [Ports & Registries](./ports-and-registries.md#3-structured-logging-croppylogger) |
| `protection` / `economy` / `i18n` | `ProtectionPort`, `ProtectionAction`, `EconomyPort`, `TranslationProvider`, `MessageKey`, `LocaleRef` | [Ports & Registries](./ports-and-registries.md#4-spi-ports) |
| `identifier` / `world` | `Key`, `Keyed`, `BlockPosition`, `ChunkPosition` | Below |
| Domain definitions | `species`, `crop`, `item`, `fertilizer`, `fuel`, `station`, `station.component`, `station.visual`, `process`, `recipe`, `crafting`, `discovery`, `menu`, `rule`, `interaction`, `storage`, `resourcepack`, `update` | [Ports & Registries](./ports-and-registries.md#2-domain-registries-via-addoncontext) |

---

## 4. Core Value Types (`identifier` & `world`)

### `Key` & `Keyed` (`dev.darkblade.croppy.api.identifier`)

Every domain object registered in Croppy implements `Keyed` (`Key getKey()`). Keys follow Minecraft-style `<namespace>:<value>` semantics:

* **Namespace regex**: `^[a-z0-9_.-]+$`
* **Value regex**: `^[a-z0-9/._-]+$`
* **Default namespace**: `"croppy"` (`Key.DEFAULT_NAMESPACE`)

```java
Key customCrop = Key.of("arcane", "moonleaf");
Key coreWheat  = Key.croppy("wheat");
Key parsed     = Key.parse("arcane:herbs/moonleaf"); // defaults to "croppy" if ':' omitted
String raw     = customCrop.asString();              // "arcane:moonleaf"
```

### Spatial Coordinates (`dev.darkblade.croppy.api.world`)

Domain methods accept pure coordinate records instead of Bukkit `Location` or `Chunk` objects:

* **`BlockPosition(String worldName, int x, int y, int z)`** — Helpers: `BlockPosition.of(worldName, x, y, z)`, `add(dx, dy, dz)`, `up()`, `down()`, and `toChunkPosition()`.
* **`ChunkPosition(String worldName, int x, int z)`** — Helper: `ChunkPosition.of(worldName, x, z)`.
