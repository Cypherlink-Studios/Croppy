---
title: Comandos y Permisos
description: Referencia completa de comandos y permisos de Croppy implementados bajo el framework Incendo Cloud 2.0.
sidebar:
  order: 4
---

# Comandos y Permisos

Croppy cuenta con una interfaz de comandos basada en **Incendo Cloud 2.0**, con autocompletado enriquecido mediante tabulación, filtrado estricto de permisos y respuestas formateadas en MiniMessage.

El comando raíz es `/croppy`.

---

## 1. Comandos para Jugadores

| Comando | Permiso | Descripción |
|---|---|---|
| `/croppy recipes` | *Ninguno (Todos)* | Abre la interfaz del Compendio Culinario. |
| `/croppy recipes <categoría>` | *Ninguno (Todos)* | Abre el compendio filtrado por una categoría (`bakery`, `soups`, `cooking`, `beverages`, `preserves`, `all`). |

---

## 2. Comandos Administrativos (`croppy.admin`)

Todos los subcomandos de gestión y depuración requieren el permiso **`croppy.admin`**.

### Catálogo Interactivo de Ítems y Spawner

```text
/croppy items [categoría | search | clear | next | prev] [query]
```

* **`/croppy items`**:
  Abre la interfaz interactiva del Catálogo de Ítems mostrando todos los ítems registrados.
* **`/croppy items <categoría>`**:
  Abre el catálogo filtrado directamente por pestaña de categoría (`all`, `seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`).
* **`/croppy items search [query]`**:
  Abre el catálogo filtrado por término de búsqueda, o inicia un prompt en el chat si se omite.
* **`/croppy items clear`**:
  Restablece todos los filtros activos y términos de búsqueda regresando a la primera página.

**Atajos del Spawner Administrativo:**
* **Clic Izquierdo**: Da 1 unidad (1x) al inventario.
* **Clic Derecho**: Da 1 stack completo (hasta el máximo del ítem, ej. 64x).
* **Shift + Clic**: Sugiere `/croppy item give <categoría> <id> 1` pre-escrito en el chat.
* **Tecla Q (Drop)**: Suelta 1 unidad en el suelo a los pies del jugador.
* **Control + Q [Ctrl+Drop]**: Suelta 1 stack completo en el suelo a los pies del jugador.

Permiso: `croppy.admin.items` (incluido en `croppy.admin`).

---

### Gestión de Ítems y Herramientas

```text
/croppy item give <categoría> <item_id> [cantidad]
/croppy item list [categoría]
```

* **`/croppy item give <categoría> <item_id> [cantidad]`**:
  Entrega ítems custom directamente en el inventario del jugador.
  * Categorías: `seeds`, `watering_cans`, `fertilizers`, `tools`, `stations`, `misc`.
  * Ejemplos:
    * `/croppy item give watering_cans copper_can 1`
    * `/croppy item give seeds wheat_seeds 16`
    * `/croppy item give tools inspector 1`
    * `/croppy item give misc recipe_book 1`
* **`/croppy item list [categoría]`**:
  Lista en el chat todos los ítems registrados en el sistema, con filtro opcional por categoría.

---

### Inspección de Suelo y Cultivos

```text
/croppy inspect
/croppy inspect <x> <y> <z>
```

* **`/croppy inspect`**:
  Inspecciona el bloque al que apunta la mirada del jugador (hasta 5 bloques) y muestra una tarjeta detallada del cultivo y de la tierra.
* **`/croppy inspect <x> <y> <z>`**:
  Inspecciona las coordenadas exactas indicadas.

---

### Simulación y Depuración del Cultivo

```text
/croppy water
/croppy fertilize <fertilizer_id>
/croppy setstage <índice_etapa>
/croppy setprogress <porcentaje_progreso>
```

* **`/croppy water`**:
  Hidrata instantáneamente la tierra arada seleccionada a su nivel máximo (`8/8`).
* **`/croppy fertilize <fertilizer_id>`**:
  Aplica el fertilizante especificado (ej. `croppy:growth_fertilizer`, `croppy:quality_fertilizer`) sobre el bloque de tierra.
* **`/croppy setstage <índice_etapa>`**:
  Fuerza el cultivo a una fase específica de su ciclo (de 0 al máximo de la especie), actualizando de inmediato su modelo o estado de bloque.
* **`/croppy setprogress <porcentaje_progreso>`**:
  Modifica el progreso acumulado del cultivo (de 0.0 a 100.0%).

---

### Gestión de Recetas

```text
/croppy recipe list
/croppy recipe give <jugador> <recipe_id>
/croppy recipe revoke <jugador> <recipe_id>
```

* **`/croppy recipe list`**:
  Muestra todas las recetas cargadas en el `RecipeRegistry`.
* **`/croppy recipe give <jugador> <recipe_id>`**:
  Desbloquea directamente una receta en el recetario del jugador (`UNLOCKED`).
* **`/croppy recipe revoke <jugador> <recipe_id>`**:
  Revoca el conocimiento de la receta, devolviéndola al estado `LOCKED`.

---

### Gestión del Ciclo de Vida de Addons

```text
/croppy addon list
/croppy addon status <addonId>
/croppy addon load <addonId | all>
/croppy addon enable <addonId | all>
/croppy addon disable <addonId | all>
/croppy addon unload <addonId | all>
```

* **`/croppy addon list`**:
  Lista todos los addons descubiertos en `plugins/Croppy/addons/`, indicando su versión, tipo (addon de código Java o pack declarativo) y estado de ciclo de vida.
* **`/croppy addon status <addonId>`**:
  Inspecciona metadatos detallados, clases cargadas, especies/ítems registrados, listeners activos y métricas de tareas del addon especificado.
* **`/croppy addon load <addonId | all>`**:
  Prepara un addon en memoria y enlaza su jerarquía de classloaders sin activar mecánicas de juego (estado `LOADED`).
* **`/croppy addon enable <addonId | all>`**:
  Habilita el addon, resuelve dependencias, registra su contenido en los registros centrales y ejecuta `onEnable` (estado `ENABLED`).
* **`/croppy addon disable <addonId | all>`**:
  Ejecuta de forma segura el protocolo de desmantelamiento de 5 fases, desregistrando listeners y limpiando registros de dominio mientras conserva las clases en memoria (estado `LOADED`).
* **`/croppy addon unload <addonId | all>`**:
  Deshabilita por completo el addon, cierra su `URLClassLoader` y desacopla todas las referencias del grafo (estado `ON_DISK`).

---

### Recarga del Plugin

```text
/croppy reload
```

* **`/croppy reload`**:
  Recarga `config.yml`, especies (`crops/`), estaciones (`stations/`), ítems, menús y recetas sin necesidad de reiniciar el servidor.

---

## 3. Resumen de Nodos de Permiso

| Permiso | Predeterminado | Descripción |
|---|---|---|
| `croppy.admin` | `op` | Acceso completo a todos los subcomandos administrativos y herramientas de depuración de `/croppy`. |
| `croppy.admin.items` | `op` | Acceso al catálogo interactivo de ítems y spawner administrativo (`/croppy items`). |
| `croppy.admin.addon` | `op` | Acceso completo a los subcomandos de gestión del ciclo de vida de `/croppy addon`. |
