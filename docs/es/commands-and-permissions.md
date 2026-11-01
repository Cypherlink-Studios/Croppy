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
  Aplica el fertilizante especificado (ej. `croppy:speed_gro`, `croppy:quality_fertilizer`) sobre el bloque de tierra.
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
