---
title: Resumen de la API de Desarrollo
description: Introducción a croppy-api, límites hexagonales, configuración en Gradle, mapa de paquetes y tipos de valor base del dominio.
sidebar:
  order: 1
---

# API de Desarrollo de Croppy (`croppy-api`)

El módulo **`croppy-api`** expone contratos puros de **Java 21**, DTOs inmutables, puertos SPI e interfaces de registro para extender Croppy sin acoplarse a la implementación interna del motor (`croppy-core`) ni a detalles de plataforma (`croppy-paper`).

---

## 1. Arquitectura Hexagonal y Límites Modulares

```text
+-----------------------------------------------------------------------+
|                              croppy-api                               |
|   Cero dependencias externas (SOLO biblioteca estándar Java 21)       |
|   * Contratos: CroppyAddon, AddonContext, AddonConfig, VariableProvider|
|   * Tipos: DataType<T>, TypedVariableStore, Key, BlockPosition        |
|   * Puertos: ProtectionPort, EconomyPort, TranslationProvider, Logger |
+-----------------------------------+-----------------------------------+
                                    | implementado por
         +--------------------------+--------------------------+
         v                                                     v
+-----------------------------------+     +-----------------------------+
|            croppy-core            |     |       Addons Externos       |
|   Motor de dominio puro y códecs  |     |   JARs Java y Extensiones   |
+-----------------------------------+     +-----------------------------+
                                    | conectado por
                                    v
+-----------------------------------------------------------------------+
|                             croppy-paper                              |
|   Adaptador Paper 1.21.8+, displays PacketEvents, eventos, SQLite     |
+-----------------------------------------------------------------------+
```

* **`croppy-api` no contiene dependencias de Bukkit, Paper, Kyori Adventure ni YAML.** Los addons pueden depender únicamente de `croppy-api` para pruebas unitarias puras y añadir `paper-api` solo cuando requieran listeners de Bukkit o adaptadores de plataforma.
* Las utilidades específicas de plataforma (como registrar `Listener` de Bukkit o rastrear `BukkitTask`) se obtienen explícitamente mediante `AddonContext.getPlatformAdapter(PaperAddonContext.class)`.

---

## 2. Configuración en Gradle

Apunta a Java **21** y declara `croppy-api` (y `paper-api` si tu addon escucha eventos de Bukkit/Paper) como dependencias `compileOnly`:

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

## 3. Mapa de Paquetes (`dev.darkblade.croppy.api`)

| Paquete | Contratos y Records Principales | Documentación |
|---|---|---|
| `addon` | `CroppyAddon`, `AddonContext`, `AddonConfig`, `AddonDescription`, `AddonManager`, `AddonState` | [Desarrollo de Addons](./addon-development.md) |
| `growth` | `GrowthModifier`, `GrowthContext`, `CropEnvironment`, `StageAdvanceInterceptor`, `UnwateredPolicy`, `WitherHandler` | [Desarrollo de Addons](./addon-development.md) |
| `variable` | `DataType<T>`, `SimpleDataType<T>`, `CompoundDataType<T>`, `DataTypeRegistry`, `TypedVariableStore`, `TypedVariable<T>`, `VariableOperation<T>`, `OperationArgs`, `UnresolvedVariableData` | [Variables Tipadas](./typed-variables.md) |
| `expression` | `VariableProvider`, `EvaluationContext`, `Expression`, `VariableResolver` | [Expresiones y Proveedores](./expressions-and-providers.md) |
| `registry` | `Registry<T extends Keyed>`, `Service` | [Puertos y Registros](./ports-and-registries.md) |
| `logging` | `CroppyLogger`, `LogLevel`, `LogChannel`, `JulCroppyLogger`, `NoOpCroppyLogger` | [Puertos y Registros](./ports-and-registries.md#3-logging-estructurado-croppylogger) |
| `protection` / `economy` / `i18n` | `ProtectionPort`, `ProtectionAction`, `EconomyPort`, `TranslationProvider`, `MessageKey`, `LocaleRef` | [Puertos y Registros](./ports-and-registries.md#4-puertos-spi) |
| `identifier` / `world` | `Key`, `Keyed`, `BlockPosition`, `ChunkPosition` | Abajo |
| Definiciones de dominio | `species`, `crop`, `item`, `fertilizer`, `fuel`, `station`, `station.component`, `station.visual`, `process`, `recipe`, `crafting`, `discovery`, `menu`, `rule`, `interaction`, `storage`, `resourcepack`, `update` | [Puertos y Registros](./ports-and-registries.md#2-registros-de-dominio-en-addoncontext) |

---

## 4. Tipos Base de Valor (`identifier` y `world`)

### `Key` y `Keyed` (`dev.darkblade.croppy.api.identifier`)

Cada objeto de dominio registrado en Croppy implementa `Keyed` (`Key getKey()`). Las claves siguen el formato estilo Minecraft `<namespace>:<value>`:

* **Regex de namespace**: `^[a-z0-9_.-]+$`
* **Regex de valor**: `^[a-z0-9/._-]+$`
* **Namespace por defecto**: `"croppy"` (`Key.DEFAULT_NAMESPACE`)

```java
Key customCrop = Key.of("arcane", "moonleaf");
Key coreWheat  = Key.croppy("wheat");
Key parsed     = Key.parse("arcane:herbs/moonleaf"); // usa "croppy" si se omite ':'
String raw     = customCrop.asString();              // "arcane:moonleaf"
```

### Coordenadas Espaciales (`dev.darkblade.croppy.api.world`)

Los métodos del dominio reciben records puros en lugar de objetos `Location` o `Chunk` de Bukkit:

* **`BlockPosition(String worldName, int x, int y, int z)`** — Utilidades: `BlockPosition.of(worldName, x, y, z)`, `add(dx, dy, dz)`, `up()`, `down()` y `toChunkPosition()`.
* **`ChunkPosition(String worldName, int x, int z)`** — Utilidad: `ChunkPosition.of(worldName, x, z)`.
