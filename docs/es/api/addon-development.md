---
title: Desarrollo de Addons en Java
description: Guía técnica de CroppyAddon, AddonContext, AddonConfig, resolución DAG de ClassLoader, hot-swapping en 5 fases y hooks de crecimiento.
sidebar:
  order: 2
---

# Desarrollo de Addons en Java

Los addons en Java extienden Croppy con listeners propios, tareas asíncronas, modificadores de crecimiento, tipos de variables personalizadas y proveedores de variables para componentes, todo ejecutado dentro de entornos aislados `AddonClassLoader` con recarga segura sin fugas de memoria.

> Para packs declarativos en YAML, formato del manifiesto `addon.yml` y comandos administrativos `/croppy addon`, consulta la [Guía del Sistema de Addons](../addon-system.md).

---

## 1. Grafo de ClassLoaders y Delegación Horizontal

Los addons de código Java se cargan mediante `AddonClassLoader`, un `URLClassLoader` especializado:

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
                                      | (Padre)
               +----------------------+----------------------+
               |                                             |
               v                                             v
+-----------------------------+               +-----------------------------+
|    AddonClassLoader [A]     | ------------> |    AddonClassLoader [B]     |
| (ej: "croppy-culinary-base")|  Delegación   |  (ej: "pastry-expansion")   |
+-----------------------------+  Horizontal   +-----------------------------+
```

Cuando un addon intenta resolver una clase:
1. **Búsqueda Local**: Verifica `findLoadedClass` y busca en el propio JAR del addon.
2. **Delegación Horizontal**: Si no la encuentra localmente, consulta el `AddonClassLoader` de cada addon declarado bajo `dependencies:` o `softDependencies:` en `addon.yml`, permitiendo al Addon B acceder a clases exportadas por el Addon A.
3. **Respaldo al Padre**: Si está ausente en todas las dependencias, delega al ClassLoader padre de Croppy (`croppy-api`, Paper, runtime de Java).

Las dependencias circulares (ej. `A -> B -> A`) se detectan durante el arranque mediante el algoritmo de Kahn y se rechazan de forma limpia.

---

## 2. Ciclo de Vida y Hot-Swapping Seguro

Los addons transitan por tres valores de `AddonState`: `ON_DISK` → `LOADED` → `ENABLED`.

### Protocolo de Desmantelamiento en 5 Fases

Para evitar fugas de Metaspace en la JVM, listeners fantasma y hilos huérfanos al ejecutar `/croppy addon disable` o `/croppy addon unload`, Croppy ejecuta un desmontaje determinista en 5 fases:

```text
[ Fase 1: Notificación ]    ──> Invoca CroppyAddon.onDisable(AddonContext).
         |
         v
[ Fase 2: Desregistro ]     ──> Paper: HandlerList.unregisterAll() en listeners rastreados
         |                      y cancelación de todas las BukkitTask rastreadas.
         v
[ Fase 3: Drenado de Hilos ] ─> ManagedExecutorService.shutdownNow() con timeout de 2s.
         |
         v
[ Fase 4: Purga Registros ] ──> Desregistro atómico de especies, ítems, recetas, recetas de
         |                      crafteo, categorías, procesos, estaciones, menús,
         |                      interacciones y modificadores de crecimiento del addon.
         v
[ Fase 5: Corte y GC ]      ──> (Solo Unload) Cierra URLClassLoader y limpia referencias
                                del grafo para que el GC reclame memoria Metaspace.
```

### Seguridad de Cultivos Latentes (Dormant)

Si un addon se deshabilita o descarga mientras sus cultivos personalizados siguen plantados en el mundo, `GrowthEngine` marca automáticamente esas instancias como **latentes (dormant)** y las omite durante los ticks de crecimiento sin lanzar excepciones ni corromper chunks. Al rehabilitar el addon, los cultivos retoman su ciclo normalmente.

---

## 3. Implementación de `CroppyAddon` y `AddonContext`

Tu clase `main` en `addon.yml` debe ser `public`, tener un constructor sin argumentos e implementar `dev.darkblade.croppy.api.addon.CroppyAddon`:

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
        // Extrae config.yml empaquetado si no existe y lee ajustes tipados
        context.saveDefaultConfig();
        AddonConfig config = context.getConfig();
        double bonusMultiplier = config.getDouble("moonlight.multiplier", 1.75);

        context.getCroppyLogger().info(LogChannel.ADDON,
                "Arcane Farming v" + context.getDescription().version() + " inicializado!");

        // Registrar modificador de crecimiento (ej. acelerar en luz baja con alta humedad)
        GrowthModifier moonlightModifier = ctx ->
                (ctx.environment().lightLevel() <= 7 && ctx.environment().waterFluidLevel() >= 4)
                        ? bonusMultiplier
                        : 1.0;

        // Rastrear listeners y hooks vía PaperAddonContext para hot-swapping seguro
        context.getPlatformAdapter(PaperAddonContext.class).ifPresent(paperCtx -> {
            paperCtx.trackGrowthModifier(moonlightModifier);
            paperCtx.registerListener(this);
        });

        // Trabajo en segundo plano drenado automáticamente al desactivar
        context.runAsync(() ->
                context.getCroppyLogger().debug(LogChannel.ADDON, () -> "Cálculo arcano asíncrono completado."));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Desregistrado automáticamente al deshabilitar/descargar
    }

    @Override
    public void onDisable(AddonContext context) {
        context.getCroppyLogger().info(LogChannel.ADDON, "Arcane Farming deshabilitado correctamente.");
    }
}
```

### Capacidades de `AddonContext`

* **Metadatos y Archivos**: `getDescription()` (`AddonDescription`), `getDataFolder()` (`Path`).
* **Logging**: `getLogger()` (`Logger` JUL conectado) y `getCroppyLogger()` (`CroppyLogger` acotado a `Addon:<id>`).
* **Configuración**: `getConfig()`, `saveDefaultConfig()`, `reloadConfig()`, `saveConfig()`.
* **Concurrencia**: `runAsync(Runnable)` envía tareas a un ejecutor daemon drenado automáticamente al deshabilitar.
* **Registros de Dominio**: 12 registros de definiciones base más `ItemMetadataRegistry` y `StationActionRegistry` (ver [Puertos y Registros](./ports-and-registries.md#2-registros-de-dominio-en-addoncontext)).
* **Adaptador de Plataforma**: `getPlatformAdapter(PaperAddonContext.class)` expone `registerListener(Listener)`, `trackTask(BukkitTask)`, `trackGrowthModifier(GrowthModifier)` y métodos `track*` para claves registradas manualmente.

---

## 4. Configuración del Addon (`AddonConfig`)

`dev.darkblade.croppy.api.addon.AddonConfig` provee una abstracción YAML pura en Java 21 respaldada en Paper por la configuración con preservación de comentarios de Bukkit (con aislamiento child-first para `config.yml`):

| Método | Tipo de Retorno | Comportamiento |
|---|---|---|
| `getString(path)` / `getString(path, def)` | `String` | Retorna `null` o `def` si está ausente. |
| `getInt(path)` / `getInt(path, def)` | `int` | Por defecto `0` o `def`. |
| `getLong(path)` / `getLong(path, def)` | `long` | Por defecto `0L` o `def`. |
| `getDouble(path)` / `getDouble(path, def)` | `double` | Por defecto `0.0` o `def`. |
| `getBoolean(path)` / `getBoolean(path, def)` | `boolean` | Por defecto `false` o `def`. |
| `getStringList(path)` | `List<String>` | Lista inmutable (vacía si está ausente). |
| `contains(path)` | `boolean` | Comprueba si existe la clave separada por puntos. |
| `getKeys(deep)` | `Set<String>` | Lista claves superficiales o recursivas de la sección actual. |
| `getSection(path)` | `Optional<AddonConfig>` | Navega a una subsección anidada. |
| `set(path, value)` | `void` | Modifica un valor en memoria (`null` elimina la clave). |
| `reload()` / `save()` | `void` | Recarga desde o persiste hacia disco. |

---

## 5. Hooks Agrícolas de Crecimiento (`dev.darkblade.croppy.api.growth`)

Los contratos puros de dominio permiten inspeccionar e influir en la simulación de cultivos (ver [Crecimiento y Entorno](../growth-and-environment.md)):

* **`GrowthModifier`**: `@FunctionalInterface` que calcula `double calculateModifier(GrowthContext context)` (`1.0` = normal, `> 1.0` = acelerado, `< 1.0` = ralentizado, `0.0` = detenido).
* **`GrowthContext(CropInstance crop, Species species, CropEnvironment environment, double baseProgress)`**: Instantánea inmutable entregada a cada modificador.
* **`CropEnvironment(int lightLevel, int waterFluidLevel, boolean isLoaded)`**: Luz ambiental del bloque (`0–15`), nivel de fluido de agua cercano (`0–8`) y estado de carga del chunk.
* **`StageAdvanceInterceptor`**: `@FunctionalInterface` (`shouldAdvance(crop, species, fromStage, toStage)`) para aprobar o vetar transiciones de etapa.
* **`UnwateredPolicy`**: Record inmutable con fábricas `pause()`, `slow(multiplier)`, `decay(amount)` y `wither(maxDryDays)`.
* **`WitherHandler`**: `@FunctionalInterface` (`onWither(crop, species)`) invocado cuando un cultivo excede su umbral de días sin riego.

---

## 6. Puertos de Extensión para Addons (`DatabaseService`, `ProcessListener`, `ItemMetadataContributor`, `StationActionContributor`)

`AddonContext` expone acceso directo a persistencia independiente de plataforma, hooks del ciclo de vida de procesos, acciones de menú en estaciones y preservación de metadatos de ítems (recuerda desregistrar tus contribuidores de `ItemMetadataRegistry` y `StationActionRegistry` en `onDisable`):

* **Pool SQLite Aislado (`getDatabaseService()`)**: Devuelve un `DatabaseService` acotado al prefijo sanitizado del addon (`addon_<id>_`). Usa `table("mi_tabla")` (validado con `[a-zA-Z0-9_]+`) junto con `execute(...)`, `query(...)` o `getConnection()` para compartir el pool HikariCP de Croppy sin colisiones.
* **Hooks de Ciclo de Vida (`registerProcessListener(ProcessListener)`)**: Suscribe callbacks a `ProcessEngine` (`onProcessStart`, `onProcessTick`, `onPhaseAdvance`, `onProcessComplete`, `onProcessFail`). Las excepciones en listeners quedan aisladas y se desregistran automáticamente al deshabilitar el addon.
* **Metadatos y Calidad de Ítems (`getItemMetadataRegistry()`, `ItemMetadataContributor`)**: Registra contribuidores que serializan `StoredItem.properties()` en tags PDC (`serializeMetadata`), sobrescriben `item_model` (`resolveItemModel`) y nombre visible MiniMessage (`resolveDisplayName`), añaden lore MiniMessage (`formatLore`) y restauran propiedades de dominio en `ItemFactory.toStoredItem(ItemStack)` (`deserializeMetadata`), preservando metadatos al sincronizar slots de estaciones.
* **Acciones de Menú en Estaciones (`getStationActionRegistry()`, `StationActionContributor`)**: Registra contribuidores para respaldar `START_PROCESS`, interceptar `EXPERIMENT` / `DISCOVER_RECIPE` o manejar acciones personalizadas de menú en estaciones con retroalimentación MiniMessage opcional (`Optional<String>`).
* **Orden Determinista de Carga en JARs**: Los YAML empaquetados en JARs de addons se cargan en orden de dependencias (`items/` → `crops/` → `stations/` → `processes/` → `recipes/` → `menus/` → `interactions/` → `crafting/`).
* **Variables Tipadas y Descubrimiento (`getDataTypeRegistry()`, `getRecipeDiscoveryEngine()`)**: Expone el registro de `VariableDataType` personalizados y consultas/otorgamiento programático de descubrimiento de recetas.
