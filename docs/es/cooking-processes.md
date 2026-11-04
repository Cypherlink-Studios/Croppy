---
title: Procesos Secuenciales de Cocción
description: Especificación técnica y guía YAML para procesos de cocción multifase, duraciones lineales, efectos sensoriales y hologramas dinámicos.
sidebar:
  order: 14
---

# Procesos Secuenciales de Cocción

Croppy v0.5.0 incorpora los **Procesos Secuenciales de Cocción**, un motor de máquinas de estado desacoplado que transforma la preparación culinaria desde simples temporizadores instantáneos hacia experiencias inmersivas, táctiles y multisensoriales.

Mientras que las **Recetas** (`plugins/Croppy/recipes/`) definen *qué* ingredientes se transforman en *cuál* plato final, los **Procesos** (`plugins/Croppy/processes/`) dictan *cómo* ocurre dicha transformación: tiempos lineales de cocción, umbrales térmicos, fases secuenciales, bucles sonoros rítmicos, partículas visuales y hologramas dinámicos en modo cartel.

---

## 1. Arquitectura y Ciclo de Vida del Proceso

Cada estación de cocina que ejecuta un proceso opera bajo una máquina de estados determinista que atraviesa diferentes fases en tiempo de ejecución:

```text
+-----------------------------------------------------------------------------+
|                     MÁQUINA DE ESTADOS DEL PROCESO                          |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [IDLE]             La estación espera ingredientes y condiciones válidas.  |
|     │                                                                       |
|     ▼ (Botón de Encendido 'B' o Inicio Automático)                          |
|  [RUNNING]          Fase activa: avance de ticks, partículas y bucles de audio.|
|     │                                                                       |
|     ├─► Caída Térmica ──► [PAUSADO / EN ESPERA] ──► Calor Restaurado ──► (Reanudar)
|     │                                                                       |
|     ▼ (Si la fase requiere interacción física)                              |
|  [AWAITING_INPUT]   Esperando interacción del jugador (revolver, añadir útil).
|     │                                                                       |
|     ▼ (Todas las fases completadas con éxito)                               |
|  [COMPLETED]        Despacha los platos terminados a los slots de salida.   |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### Características Principales
* **Progresión Temporal Lineal**: Simulación precisa tick a tick independiente de los ticks aleatorios del mundo (*random ticks*).
* **Pausa Térmica Segura (`pause_on_condition_failure: true`)**: Si la fuente de calor se apaga o la temperatura cae a mitad de la cocción, el progreso se congela de forma segura sin arruinar los ingredientes.
* **Inmersión Multisensorial**: Generación de partículas mediante PacketEvents y reproducción rítmica de efectos de sonido en cada etapa.
* **Holograma Flotante en Cartel**: Barra de progreso en tiempo real, porcentajes, tiempo transcurrido/restante y temperatura activa proyectados en Text Displays virtuales.

---

## 2. Desglose Completo del Esquema YAML

Las definiciones de procesos se almacenan como archivos YAML individuales dentro de la carpeta `plugins/Croppy/processes/`.

A continuación se muestra un desglose comentado de `plugins/Croppy/processes/tomato_soup_cook.yml`:

```yaml
# Identificador semántico único con namespace (<namespace>:<valor>)
id: "croppy:tomato_soup_cook"

# Nombre para mostrar del proceso
name: "Tomato Soup Cooking"

# Estación de trabajo vinculada a este proceso
station: "croppy:cooking_pot"

# Requisitos previos para permitir el encendido o inicio del proceso
start_conditions:
  - "station.temperature >= 75"

# Fases secuenciales de progresión
phases:
  - id: "simmer"
    name: "Simmering"
    type: TIMED
    duration: 15s
    
    # Condición que debe cumplirse para que el progreso avance
    condition: "station.temperature >= 75"
    
    # Si es true, el progreso se congela en lugar de abortar o arruinar el plato
    pause_on_condition_failure: true
    
    # Configuración del holograma flotante virtual en el cliente
    display:
      template: "<gray>Cociendo: <yellow>{progress_percent}%</yellow> <dark_gray>[{progress_bar}]</dark_gray> <aqua>{progress_seconds_left}s</aqua></gray>"
      bar:
        length: 10
        filled: "<gradient:#ff5555:#ffaa00>■</gradient>"
        empty: "<dark_gray>■</dark_gray>"
        
    # Efectos de partículas y sonido emitidos durante la fase
    effects:
      particles:
        particle: "SPLASH"
        count: 2
        offset: [0.15, 0.05, 0.15]
        speed: 0.02
      sound:
        sound: "BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE"
        interval: 2s
        volume: 0.4
        pitch: 1.2
```

---

## 3. Tabla de Referencia de Atributos

### Atributos Raíz del Proceso

| Campo | Tipo | Requerido | Predeterminado | Descripción |
|---|---|---|---|---|
| `id` | String (`Key`) | **Sí** | — | Identificador único del proceso (ej. `croppy:tomato_soup_cook`). |
| `name` | String | No | Valor de `id` | Nombre legible para registros de diagnóstico y títulos por defecto. |
| `station` | String (`Key`) | No | `croppy:default_station` | Clave de la estación autorizada para ejecutar este proceso. |
| `start_conditions` | String o Lista | No | Siempre Verdadero | Expresión(es) que deben cumplirse para iniciar la cocción. |
| `phases` | Lista de Mapas | **Sí** | 1 fase estándar | Lista ordenada de etapas de progresión. |
| `properties` | Mapa<String, String> | No | `{}` | Metadatos y propiedades personalizadas. |

### Atributos de Cada Fase (`phases`)

| Campo | Tipo | Requerido | Predeterminado | Descripción |
|---|---|---|---|---|
| `id` | String | **Sí** | — | Identificador único de la fase dentro del proceso. |
| `name` | String | No | ID capitalizado | Nombre de la fase accesible mediante `{phase_name}`. |
| `type` | String | No | `TIMED` | Tipo de fase: `TIMED` (por tiempo), `MANUAL_INTERACTION` (interacción manual) o `ENVIRONMENTAL` (ambiental). |
| `duration` | String / Número | No | `200t` (`10s`) | Duración de la fase. Soporta sufijos `15s`, `300t`, `2m`, `500ms`. |
| `timeout` | String / Número | No | `0t` (sin límite) | Tiempo límite antes de fallar en fases interactivas. |
| `condition` | String o Lista | No | Siempre Verdadero | Expresión evaluada cada tick; si falla, congela o cancela el avance. |
| `pause_on_condition_failure`| Booleano | No | `true` | Si es `true`, pausa el avance al perder condiciones en lugar de abortar. |
| `failure_policy` | String | No | `CANCEL` | Acción al fallar: `CANCEL`, `SPOIL` (arruinar) o `BYPRODUCT` (subproducto). |
| `byproduct` | String (`Key`) | No | — | Identificador del ítem residual producido si la fase falla. |
| `prompt` | String | No | — | Mensaje informativo o de acción mostrado durante la interacción. |
| `display.template` | String | No | Plantilla base | Plantilla MiniMessage proyectada en el holograma flotante. |
| `display.bar.length` | Entero | No | `10` | Cantidad total de caracteres en `{progress_bar}`. |
| `display.bar.filled` | String | No | `<green>■</green>` | Símbolo MiniMessage que representa el progreso completado. |
| `display.bar.empty` | String | No | `<dark_gray>■</dark_gray>`| Símbolo MiniMessage que representa el progreso pendiente. |
| `effects.particles` | Mapa | No | Ninguno | Tipo de partícula, cantidad, dispersión `[x, y, z]` y velocidad. |
| `effects.sound` | Mapa | No | Ninguno | Sonido de Bukkit, intervalo de repetición (`2s`, `40t`), volumen y tono. |

---

## 4. Formatos de Duración y Unidades de Tiempo

Las duraciones de fases y los intervalos de sonido aceptan sufijos estándar de tiempo:

| Sufijo | Unidad | Equivalencia en Ticks | Ejemplo |
|---|---|---|---|
| `s` | Segundos | `valor * 20` ticks | `10s` (200 ticks) |
| `t` | Ticks de Servidor | `valor` ticks directos | `300t` (15 segundos) |
| `m` | Minutos | `valor * 1200` ticks | `1.5m` (1800 ticks) |
| `ms` | Milisegundos | `(valor / 50)` ticks | `1500ms` (30 ticks) |
| *(Sin sufijo)* | Ticks Numéricos | Valor entero directo | `200` (200 ticks) |

---

## 5. Pausa Térmica y Retención de Progreso

Los plugins tradicionales de cocina anulan la receta por completo si la estación pierde combustible por un instante. El motor de **Retención Térmica** de Croppy resuelve este problema:

Cuando se configura `pause_on_condition_failure: true` en una fase:
1. Si la temperatura cae por debajo del umbral mínimo requerido (ej. `station.temperature >= 75`), la fase entra en estado **PAUSADO / EN ESPERA**.
2. El porcentaje acumulado y los segundos transcurridos se mantienen intactos en memoria y en disco.
3. Las partículas y los efectos de sonido se suspenden de inmediato para advertir visualmente la falta de calor.
4. Tan pronto como el jugador agregue combustible o reencienda la fogata y la temperatura alcance nuevamente `75°C`, el proceso se reanuda al instante desde el mismo punto.

---

## 6. Placeholders de Hologramas Dinámicos

Durante la cocción, el Text Display virtual de la estación proyecta información resuelta en tiempo real mediante `ProcessDisplayFormatter`. Los siguientes placeholders están disponibles en `display.template`:

| Placeholder | Ejemplo de Salida | Descripción |
|---|---|---|
| `{progress_percent}` | `45` | Porcentaje de avance de la fase actual (`0` a `100`). |
| `{progress_percent_left}` | `55` | Porcentaje invertido restante (`100` a `0`). |
| `{progress_seconds}` | `6` | Segundos transcurridos en la fase actual. |
| `{progress_seconds_left}` | `9` | Segundos restantes para completar la fase. |
| `{progress_ticks}` | `134` | Ticks exactos del servidor transcurridos en la fase. |
| `{progress_ticks_left}` | `166` | Ticks exactos del servidor restantes en la fase. |
| `{progress_bar}` | `■■■■□□□□□□` | Barra visual formateada según `bar.filled` y `bar.empty`. |
| `{phase_name}` | `Simmering` | Nombre de la fase activa. |
| `{phase_index}` | `1` | Índice secuencial de la fase (comenzando en 1). |
| `{phase_total}` | `3` | Cantidad total de fases definidas en el proceso. |
| `{recipe_name}` | `Sopa de Tomate` | Nombre de la receta culinaria que se está preparando. |
| `{temperature}` | `82.4` | Temperatura actual de la estación de cocina (°C). |

---

## 7. Retroalimentación Multisensorial (Partículas y Audio)

Para crear un entorno culinario vivo y atmosférico, las fases soportan emisiones continuas de partículas y efectos acústicos cíclicos:

### Configuración de Partículas
```yaml
effects:
  particles:
    particle: "CAMPFIRE_COSY_SMOKE" # Cualquier tipo de partícula válido en Minecraft
    count: 3                        # Cantidad de partículas por ciclo
    offset: [0.15, 0.08, 0.15]      # Dispersión en ejes X, Y, Z
    speed: 0.01                     # Velocidad de elevación o dispersión
```

### Configuración de Sonido
```yaml
effects:
  sound:
    sound: "BLOCK_FIRE_EXTINGUISH"  # Nombre de sonido Bukkit o clave namespaced
    interval: 30t                   # Frecuencia de repetición (soporta s, t, m)
    volume: 0.35                    # Volumen del efecto (0.0 a 1.0+)
    pitch: 1.4                      # Tono o afinación (0.5 a 2.0)
```

---

## 8. Tutorial Paso a Paso: Salsa Balsámica en Varias Fases

Configuremos un proceso culinario avanzado en dos etapas: **Reducción Balsámica de Tomate** (`croppy:balsamic_reduction`).

### Paso 1: Crear la Definición del Proceso
Crea `plugins/Croppy/processes/balsamic_reduction.yml`:

```yaml
id: "croppy:balsamic_reduction"
name: "Reducción Balsámica de Tomate"
station: "croppy:cooking_pot"

start_conditions:
  - "station.temperature >= 90"

phases:
  # Fase 1: Hervor rápido a fuego alto (10 segundos)
  - id: "boil"
    name: "Hervor Intenso"
    type: TIMED
    duration: 10s
    condition: "station.temperature >= 90"
    pause_on_condition_failure: true
    display:
      template: "<red>Hirviendo:</red> <yellow>{progress_percent}%</yellow> <dark_gray>[{progress_bar}]</dark_gray> <gray>({progress_seconds_left}s)</gray>"
      bar:
        length: 8
        filled: "<red>■</red>"
        empty: "<dark_gray>■</dark_gray>"
    effects:
      particles:
        particle: "SPLASH"
        count: 4
        offset: [0.2, 0.05, 0.2]
        speed: 0.05
      sound:
        sound: "BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE"
        interval: 1s
        volume: 0.6
        pitch: 1.5

  # Fase 2: Reducción lenta a fuego medio (20 segundos)
  - id: "simmer"
    name: "Reducción Lenta"
    type: TIMED
    duration: 20s
    condition: "station.temperature >= 60"
    pause_on_condition_failure: true
    display:
      template: "<gold>Reduciendo:</gold> <yellow>{progress_percent}%</yellow> <dark_gray>[{progress_bar}]</dark_gray> <gray>({progress_seconds_left}s)</gray>"
      bar:
        length: 8
        filled: "<gold>■</gold>"
        empty: "<dark_gray>■</dark_gray>"
    effects:
      particles:
        particle: "SMOKE"
        count: 2
        offset: [0.1, 0.1, 0.1]
        speed: 0.01
      sound:
        sound: "BLOCK_BREWING_STAND_BREW"
        interval: 3s
        volume: 0.4
        pitch: 0.9
```

### Paso 2: Vincular el Proceso en una Receta
En `plugins/Croppy/recipes/balsamic_sauce.yml`:

```yaml
id: "croppy:balsamic_sauce"
name: "<gradient:#aa0000:#550000>Salsa Balsámica de Tomate</gradient>"
category: "croppy:condiments"
station: "croppy:cooking_pot"

# Vinculación con el proceso personalizado
process: "croppy:balsamic_reduction"

inputs:
  - item: "croppy:tomato"
    amount: 3
  - item: "minecraft:sugar"
    amount: 1
  - item: "minecraft:glass_bottle"
    amount: 1

result:
  item: "croppy:balsamic_sauce"
  amount: 1
```

### Paso 3: Recargar y Cocinar
1. Ejecuta `/croppy reload` en el servidor.
2. Coloca los ingredientes dentro de una Olla de Cocina sobre una fuente de calor encendida.
3. Abre el menú GUI de la olla y haz clic en el botón **'B'** (Encender Utensilio).
4. ¡Observa cómo la preparación pasa dinámicamente de **Hervor Intenso** a **Reducción Lenta** con sonidos, partículas y hologramas personalizados!
