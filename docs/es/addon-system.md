---
title: Sistema de Addons y Extensibilidad
description: Guía completa de arquitectura y desarrollo del Sistema de Addons de Croppy, con soporte para packs declarativos, extensiones Java, grafo de ClassLoaders DAG y recarga en caliente segura.
sidebar:
  order: 21
---

# Sistema de Addons y Extensibilidad

Croppy incorpora un **Motor de Addons** oficial y desacoplado que permite tanto a administradores de servidores como a desarrolladores de Java expandir la simulación agrícola, cultivos, ítems, estaciones de cocina y recetas culinarias sin modificar el código base del plugin.

El sistema se rige por límites arquitectónicos estrictos:
- **Packs de Contenido Declarativo**: Definiciones puras en YAML y assets para resource packs sin necesidad de compilar código Java.
- **Addons de Código Java**: Archivos JAR modulares que implementan el punto de entrada `CroppyAddon`, ejecutados en entornos aislados de `AddonClassLoader`.
- **Grafo de ClassLoaders (DAG)**: Resolución topológica determinista de dependencias que permite delegación horizontal de clases (`Addon A -> Addon B -> Croppy Core`).
- **Protocolo de Desmantelamiento en 5 Fases**: Desactivación y descarga en caliente (`/croppy addon disable` y `/croppy addon unload`) 100% libre de fugas de memoria (Metaspace leaks), limpiando listeners de Bukkit, drenando hilos asíncronos y purgando los registros del dominio.
- **Fusión Automática de Resource Packs**: Los assets ubicados en `assets/<namespace>/` dentro de los addons se combinan automáticamente en el resource pack del servidor.

---

## 1. Modelo Arquitectónico y Jerarquía de Capas

Croppy implementa un modelo de contenido en tres capas de prioridad:

```text
+-------------------------------------------------------------------------------+
|                       JERARQUÍA DE CONTENIDO Y PRIORIDAD                      |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [ 1. OVERRIDES DEL SERVIDOR LOCAL ] (plugins/Croppy/crops/, items/, etc.)    |
|     * Prioridad Máxima: Sobrescribe cualquier definición de addon o del core. |
|     * Ajustes rápidos para administradores sin necesidad de reempaquetar.     |
|                                                                               |
|  [ 2. CAPA DE ADDONS ] (plugins/Croppy/addons/*.jar o carpetas)               |
|     * Unidades distribuibles con su propio namespace y manifiesto addon.yml.  |
|     * Pueden contener YAMLs declarativos, modelos 3D/texturas o código Java.  |
|                                                                               |
|  [ 3. NÚCLEO CROPPY DEFAULT ] (Contenido base de fábrica)                     |
|     * Cultivos y recetas estándar provistas por el plugin Croppy.             |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. El Manifiesto del Addon (`addon.yml`)

Todo addon debe declarar un archivo `addon.yml` en su raíz:

```yaml
# Identificador único del addon (minúsculas, números, guiones, puntos y guiones bajos)
id: arcane-farming

# Nombre legible para humanos
name: "Arcane Farming"

# Versión semántica
version: "1.0.0"

# Namespace principal de recursos perteneciente a este addon
namespace: arcane

# Clase principal opcional que implementa CroppyAddon.
# Si se omite o queda vacía, ¡Croppy lo trata como un Pack de Contenido Declarativo!
main: dev.darkblade.croppy.arcane.ArcaneFarmingAddon

# Metadatos opcionales de autores
authors:
  - DarkBladeDev
  - ContributorOne

# Descripción breve opcional
description: "Cultivos mágicos, fases lunares y mecánicas de infusión en altares."

# Dependencias requeridas (resueltas topológicamente en orden estricto)
dependencies:
  - croppy

# Dependencias blandas/opcionales (cargadas antes de este addon si están presentes)
softDependencies:
  - exotic-fruits
```

### Reglas de Validación
* `id` y `namespace`: Deben cumplir estrictamente con la expresión regular `^[a-z0-9_.-]+$`.
* `main`: Debe apuntar a una clase pública con constructor sin argumentos que implemente `dev.darkblade.croppy.api.addon.CroppyAddon`.
* Las dependencias circulares (`A -> B -> A`) son detectadas mediante el algoritmo de Kahn y rechazadas al inicio.

---

## 3. Grafo de ClassLoaders y Delegación Horizontal

Los addons de Java se cargan mediante `AddonClassLoader`, un `URLClassLoader` especializado:

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
| (ej: "croppy-culinary-base")|  Delegación   |  (ej: "pastry-expansion")   |
+-----------------------------+  Horizontal   +-----------------------------+
```

Cuando un addon intenta cargar una clase:
1. **Búsqueda Local**: Revisa `findLoadedClass` y busca en el propio archivo JAR del addon.
2. **Delegación Horizontal**: Si no se encuentra localmente, consulta secuencialmente el `AddonClassLoader` de cada dependencia declarada en `dependencies:` o `softDependencies:`, permitiendo al Addon B consumir clases públicas del Addon A.
3. **Fallback al Parent**: Si ninguna dependencia la contiene, delega al ClassLoader padre de Croppy (`croppy-api`, Paper, Java runtime).

---

## 4. Hot-Swapping Seguro: Protocolo de 5 Fases

Para evitar fugas de memoria en el Metaspace de la JVM, listeners de eventos huérfanos e hilos abandonados durante recargas en caliente, Croppy ejecuta un riguroso protocolo de desmantelamiento en 5 fases:

```text
[ Fase 1: Notificación ] ──> Invoca CroppyAddon.onDisable(AddonContext).
         |
         v
[ Fase 2: Desregistro ]  ──> Bukkit: HandlerList.unregisterAll() sobre todos los listeners.
         |                   Cancela todas las BukkitTasks registradas.
         v
[ Fase 3: Drenado Hilos] ──> ManagedExecutorService.shutdownNow() con timeout de 2 segundos.
         |
         v
[ Fase 4: Purga Registros]─> Desregistro atómico de especies, items, recetas, procesos,
         |                   estaciones y modificadores registrados por este addon.
         v
[ Fase 5: Cierre & GC ]  ──> (Solo en unload) Cierra el URLClassLoader y limpia referencias
                             del grafo, permitiendo al Garbage Collector liberar el Metaspace.
```

### Seguridad de Cultivos en el Mundo (Dormant State)
Si un addon se desactiva o descarga mientras sus cultivos personalizados están plantados en el mundo de Minecraft, el motor de simulación (`GrowthEngine`) marca automáticamente esas instancias como **dormidas** (*dormant*). Los ticks de crecimiento ignoran los cultivos dormidos sin arrojar `NullPointerException` ni corromper chunks. Cuando el addon vuelve a habilitarse, los cultivos despiertan y continúan su ciclo de vida con total normalidad.

---

## 5. Guía para Desarrolladores: Creando un Addon en Java

### Paso 1: Configuración en Gradle
Añade `croppy-api` como dependencia de compilación:

```kotlin
dependencies {
    compileOnly("dev.darkblade.croppy:croppy-api:0.5.7")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}
```

### Paso 2: Implementar `CroppyAddon`
Crea tu clase principal:

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
        context.getLogger().info("¡Arcane Farming inicializado!");

        // 1. Modificador de crecimiento: los cultivos crecen 2x más rápido de noche
        GrowthModifier moonlightModifier = ctx -> {
            boolean isNight = ctx.environment().time() >= 13000 && ctx.environment().time() <= 23000;
            return isNight ? 2.0 : 1.0;
        };

        // 2. Registrar listener de Bukkit mediante el adaptador de Paper
        context.getPlatformAdapter(PaperAddonContext.class).ifPresent(paperCtx -> {
            paperCtx.trackGrowthModifier(moonlightModifier);
            paperCtx.registerListener(this);
        });

        // 3. Tareas asíncronas administradas de forma segura
        context.runAsync(() -> {
            context.getLogger().info("Calculando rituales arcanos en segundo plano...");
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Manejo seguro y rastreado de eventos
    }

    @Override
    public void onDisable(AddonContext context) {
        context.getLogger().info("Arcane Farming desactivado de forma limpia.");
    }
}
```

---

## 6. Guía para Creadores de Contenido: Packs Declarativos

Un pack declarativo no requiere conocimientos de programación. Simplemente coloca tus archivos en `plugins/Croppy/addons/<tu-pack>/`:

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

Croppy se encargará automáticamente de:
1. Parsear `addon.yml`.
2. Registrar cultivos, ítems y recetas en los registros del núcleo.
3. Fusionar `assets/exotic_fruits/` en el resource pack del servidor.

---

## 7. Referencia de Comandos

Todos los comandos de gestión de addons requieren el permiso `croppy.admin.addon`:

| Comando | Descripción |
|---|---|
| `/croppy addon list` | Muestra la lista de addons descubiertos, versión, tipo (Java / Pack) y estado (`ON_DISK`, `LOADED`, `ENABLED`). |
| `/croppy addon status <addonId>` | Muestra información detallada: metadatos, clases cargadas, especies/ítems registrados y listeners activos. |
| `/croppy addon load <addonId \| all>` | Carga el manifiesto y prepara el ClassLoader en memoria (estado `LOADED`). |
| `/croppy addon enable <addonId \| all>` | Activa el addon, ejecuta `onEnable` y registra su contenido en el juego (estado `ENABLED`). |
| `/croppy addon disable <addonId \| all>` | Ejecuta el protocolo de 5 fases, desregistrando listeners y registros del juego (estado `LOADED`). |
| `/croppy addon unload <addonId \| all>` | Cierra completamente el ClassLoader y libera referencias de memoria (estado `ON_DISK`). |
