---
title: Reglas e Interacciones de Estaciones
description: Especificación técnica y referencia YAML para disparadores de interacción y reglas dinámicas de fallo de cocción con compilación de expresiones Pratt AST.
sidebar:
  order: 16
---

# Reglas e Interacciones de Estaciones

Croppy ofrece dos sistemas declarativos orientados a eventos para la automatización e implementación de mecánicas de fallo en estaciones:
1. **Interacciones de Estaciones (`plugins/Croppy/interactions/*.yml`)**: Despacha acciones personalizadas basadas en clics del jugador, ítems sostenidos y estados del bloque.
2. **Reglas Dinámicas de Cocina (`plugins/Croppy/rules/*.yml`)**: Evalúa condiciones en tiempo real mediante **expresiones Pratt AST pre-compiladas sin asignación de memoria** para desencadenar comida quemada, partículas de humo y efectos de sonido.

---

## 1. Interacciones de Estaciones (`interactions/*.yml`)

Las interacciones vinculan los eventos de entrada del jugador (como hacer clic con un termómetro culinario o clic agachado en una olla) con pipelines de acciones.

### Ejemplo Completo de Interacción YAML
A continuación se muestra la configuración para abrir la interfaz de la olla en `plugins/Croppy/interactions/cooking_pot_interactions.yml`:

```yaml
id: "croppy:cooking_pot_gui"

# Estación objetivo a la que se vincula
station: "croppy:cooking_pot"

# Evento desencadenante de clic
trigger: "SNEAK_RIGHT_CLICK"

# Prioridad de ejecución (valores mayores se evalúan primero)
priority: 100

# Condiciones opcionales evaluadas antes de ejecutar las acciones
conditions:
  - "station.is_heated == true"

# Pipeline de acciones
actions:
  - type: "OPEN_GUI"
  - type: "PLAY_SOUND"
    sound: "BLOCK_CHEST_OPEN"
    pitch: 1.2
```

### Filtros de Coincidencia de Ítems
Las interacciones pueden exigir ítems específicos en la mano principal:
```yaml
id: "croppy:thermometer_inspect"
station: "croppy:cooking_pot"
trigger: "RIGHT_CLICK"

# Coincide únicamente al sostener el termómetro culinario
match_item: "croppy:thermometer"

priority: 200

actions:
  - type: "SHOW_TEMPERATURE"
  - type: "PLAY_SOUND"
    sound: "BLOCK_NOTE_BLOCK_BELL"
    pitch: 1.5
```

### Disparadores de Interacción
* `RIGHT_CLICK`: Interacción estándar con la mano en el bloque.
* `SNEAK_RIGHT_CLICK`: Clic derecho agachado en el bloque.
* `LEFT_CLICK`: Golpe directo al bloque de la estación.
* `SNEAK_LEFT_CLICK`: Golpe agachado a la estación.

---

## 2. Reglas Dinámicas de Cocina (`rules/*.yml`)

Las reglas se evalúan de forma continua durante el ciclo de simulación de las estaciones (`STATION_TICK`). Si las condiciones resultan verdaderas (`true`), se disparan las penalizaciones y efectos configurados.

### Ejemplo Completo de Regla YAML
A continuación se presenta la regla de sobrecocción de `plugins/Croppy/rules/overcooking_burn.yml`:

```yaml
id: "croppy:overcooking_burn"
name: "Overcooking Burn"

# Ciclo de evaluación
trigger: "STATION_TICK"

# Clasificación de severidad: INFO, WARNING o CRITICAL
severity: "CRITICAL"

# Orden de prioridad
priority: 100

# Expresiones booleanas pre-compiladas mediante Pratt AST
conditions:
  - "process.finished == true"
  - "process.idle_ticks > 200"
  - "station.temperature > 120"

# Acciones de penalización y feedback
actions:
  - type: "REPLACE_OUTPUT"
    target_item: "croppy:burnt_food"
  - type: "EMIT_PARTICLES"
    particle: "LARGE_SMOKE"
    count: 8
  - type: "PLAY_SOUND"
    sound: "ENTITY_GENERIC_BURN"
```

---

## 3. Gramática del Evaluador de Expresiones Pratt AST

Las condiciones de las reglas de Croppy son pre-compiladas en un Árbol de Sintaxis Abstracta (AST) durante el arranque del servidor mediante `ExpressionCompiler`. Esto asegura **cero asignaciones en memoria heap** durante los ciclos rápidos de simulación del servidor.

```text
+-----------------------------------------------------------------------------+
|                     PIPELINE DE CONDICIONES AST SIN ASIGNACIÓN              |
+-----------------------------------------------------------------------------+
|                                                                             |
|  "process.finished == true and station.temperature > 120"                   |
|                                |                                            |
|                                v  (Compilado por YamlRuleLoader al iniciar) |
|                     [Operador Binario: AND]                                 |
|                    /                       \                                |
|       [Comparación: ==]                 [Comparación: >]                    |
|       /               \                 /              \                    |
|  [process.finished] [true]      [station.temperature]  [120]                |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### Operadores Soportados
* **Igualdad y Comparación**: `==`, `!=`, `<`, `<=`, `>`, `>=`
* **Conectores Lógicos**: `and`, `&&`, `or`, `||`, `not`, `!`
* **Agrupación**: Paréntesis `( ... )` para alterar la precedencia de evaluación.

### Variables Contextuales Disponibles

| Variable | Tipo | Descripción |
|---|---|---|
| `process.finished` | Booleano | `true` cuando el progreso de cocción alcanza el 100%. |
| `process.progress` | Número | Progreso activo de cocción entre `0.0` y `1.0`. |
| `process.idle_ticks` | Número | Ticks transcurridos desde que el plato se completó sin ser retirado. |
| `station.temperature` | Número | Temperatura superficial actual en grados Celsius. |
| `station.is_heated` | Booleano | `true` si la temperatura está por encima de la temperatura ambiente (>25°C). |
| `station.empty` | Booleano | `true` si la estación no contiene ingredientes ni platos terminados. |

---

## 4. Referencia de Acciones de Regla

| Tipo de Acción | Parámetros Clave | Descripción |
|---|---|---|
| `REPLACE_OUTPUT` | `target_item: String` | Reemplaza los ítems de salida de la estación por otro ítem (ej. `croppy:burnt_food`). |
| `CANCEL_PROCESS` | — | Restablece inmediatamente el progreso de cocción a 0%. |
| `EMIT_PARTICLES` | `particle: String`, `count: Int` | Emite partículas en el centro del bloque (ej. `LARGE_SMOKE`, `FLAME`, `CAMPFIRE_COSY_SMOKE`). |
| `PLAY_SOUND` | `sound: String`, `pitch: Float`, `volume: Float` | Reproduce un efecto sonoro posicional para jugadores cercanos (ej. `ENTITY_GENERIC_BURN`). |
| `DROP_INPUTS` | — | Expulsa todos los ingredientes al suelo como entidades de ítem en el mundo. |
