---
title: Configuración de Estaciones de Trabajo
description: Especificación técnica y guía de configuración YAML para estaciones físicas de cocina, displays virtuales 3D de PacketEvents y curvas termodinámicas.
sidebar:
  order: 10
---

# Configuración de Estaciones de Trabajo

Las estaciones de trabajo culinarias en Croppy son bloques físicos situados en el mundo de Minecraft que interactúan con recetas, fuentes de calor y jugadores. Se configuran como archivos modulares YAML en el directorio `plugins/Croppy/stations/`.

El motor analiza estas definiciones mediante `YamlStationLoader`, construyendo instancias inmutables de `StationDefinition` que controlan los modos de interacción, capacidades de almacenamiento, entidades virtuales cliente con PacketEvents y comportamiento termodinámico.

---

## 1. Desglose Completo del Esquema YAML

A continuación se presenta un ejemplo exhaustivo y completamente documentado que representa la configuración moderna con modelos base, vistas previas de procesos, hologramas de texto y estados dinámicos (`plugins/Croppy/stations/cooking_pot.yml`):

```yaml
# Clave semántica única con namespace de la estación
id: "croppy:cooking_pot"

# Nombre para mostrar con formato de color MiniMessage
name: "<gradient:#ffaa00:#ff5555>Olla de Cocina</gradient>"

# Bloque físico de Minecraft que representa esta estación en el mundo
block: "minecraft:barrier"

# Modo de orientación direccional al colocar la estación: CARDINAL_4, CARDINAL_8, o FREE_YAW
rotation_mode: "CARDINAL_4"

# Paradigma de interacción: PHYSICAL, GUI o HYBRID
interaction: "HYBRID"

# Capacidad de ranuras de entrada y salida
input_capacity: 6
output_capacity: 2

# Clave del menú de inventario asociado en plugins/Croppy/menus/
menu: "croppy:cooking_pot"

# Interruptor maestro para los displays virtuales cliente con PacketEvents
has_display: true

# Capas visuales cliente mediante entidades virtuales PacketEvents
visual:
  # Oculta el bloque físico del mundo mediante paquetes de barrera para evitar colisiones visuales
  hide_base_block: true

  # Dimensiones de la entidad virtual INTERACTION para detección de clics del jugador
  interaction:
    width: 1.01
    height: 1.01

  # Display 3D del Modelo Base: El recipiente físico predeterminado
  base_model:
    enabled: true
    type: "ITEM"
    item: "croppy:cooking_pot"
    translation: [0.0, 0.5, 0.0]
    scale: [1.0, 1.0, 1.0]
    rotation: [0.0, 0.0, 0.0]
    billboard: "FIXED"
    view_distance: 48
    interpolation_duration: 0

  # Display 3D de Ítems en Proceso: Renderiza los ingredientes flotando dentro de la olla
  process_display:
    enabled: true
    type: "ITEM"
    translation: [0.0, 0.35, 0.0]
    scale: [0.55, 0.15, 0.55]
    view_distance: 24
    interpolation_duration: 3

  # Holograma de Texto Flotante: Estado en tiempo real sobre la estación
  text_display:
    enabled: true
    translation: [0.0, 1.35, 0.0]
    billboard: "CENTER"
    view_distance: 8
    line_width: 220
    hide_when_idle: true
    format: |
      <gradient:#ffaa00:#ff5555><bold>{recipe_name}</bold></gradient>
      {progress_bar} <white>{progress_percent}%</white>
      <gray>Calor: <aqua>{temperature_c}°C</aqua> (<gold>{thermal_zone}</gold>)</gray>

# Estados Dinámicos y Reactivos (evaluados en tiempo real por prioridad descendente)
states:
  - id: "boiling"
    priority: 30
    when: "temperature >= 100.0 && process_state == 'RUNNING'"
    model:
      item_model: "croppy:station/cooking_pot/boiling"
      scale: [1.02, 1.05, 1.02]
    hitbox:
      width: 1.01
      height: 1.10
    effects:
      particle:
        type: "SPLASH"
        count: 3
        offset: [0.15, 0.45, 0.15]
        speed: 0.02
      sound:
        sound: "block.bubble_column.upwards_inside"
        interval_ticks: 30
        volume: 0.6
        pitch: 1.2

  - id: "cooking"
    priority: 20
    when: "process_state == 'RUNNING'"
    model:
      item_model: "croppy:station/cooking_pot/cooking"
    effects:
      particle:
        type: "SMOKE"
        count: 2
        offset: [0.1, 0.5, 0.1]
        speed: 0.01

  - id: "has_liquid"
    priority: 10
    when: "has_inputs == true"
    model:
      item_model: "croppy:station/cooking_pot/water"

  - id: "idle"
    priority: 0
    model:
      item_model: "croppy:cooking_pot"

# Propiedades operativas y térmicas
properties:
  thermal: "true"
  has_thermal: "true"
```

---

## 2. Modos de Orientación y Rotación Dinámica

Al colocar un bloque de estación de trabajo en el mundo, el motor calcula dinámicamente el yaw de colocación para que el frente de la estación mire siempre hacia el jugador (`playerYaw + 180°`).

El comportamiento de ajuste se controla mediante el atributo `rotation_mode` (alias: `rotation`):

| Modo de Rotación | Ángulo de Ajuste | Descripción |
|---|---|---|
| `CARDINAL_4` | Intervalos de 90° | Se ajusta a Norte, Sur, Este u Oeste. Óptimo para bloques cúbicos estándar. *(Por defecto)* |
| `CARDINAL_8` | Intervalos de 45° | Se ajusta a los 8 puntos cardinales (cardinales + diagonales). Ideal para mostradores en ángulo. |
| `FREE_YAW` | Continuo (0.0° - 360.0°) | Conserva el ángulo horizontal continuo exacto opuesto al jugador. |

### Sincronización Visual y Persistencia
* **Display del Modelo Base**: En modelos 3D virtuales gestionados por PacketEvents, el yaw de la estación se aplica directamente al cuaternión de rotación de la transformación.
* **Traslación de Ingredientes Hijos**: Los desplazamientos locales `[x, y, z]` de los Item Displays de ingredientes en proceso se rotan en tiempo real según el yaw de la estación para que giren solidariamente con el recipiente.
* **Sincronización de Bloques**: Al usar `minecraft:player_head`, el tile entity del bloque de cabeza colocado en el mundo actualiza su rotación y textura.
* **Persistencia Binaria**: El yaw de la estación se almacena mediante el `StationBinaryCodec` (Esquema V4), manteniéndose intacto tras descargas de chunks y reinicios del servidor.

---

## 3. Estaciones Generadoras de Calor (`heat_source`)

Las estufas y hogares de combustión declaran curvas termodinámicas en lugar de recetas. A continuación se detalla la configuración de `plugins/Croppy/stations/stove.yml`:

```yaml
id: "croppy:stove"
name: "<gradient:#ff8800:#ff3300>Estufa Rústica</gradient>"
block: "minecraft:smoker"
rotation_mode: "CARDINAL_4"
interaction: "HYBRID"
input_capacity: 1
output_capacity: 0
menu: "croppy:stove_menu"
has_display: false

properties:
  # Identifica esta estación como generador térmico
  heat_source: "true"
  
  # Temperatura ambiente cuando está apagada
  min_temperature: "20.0"
  
  # Límite térmico máximo alcanzable en grados Celsius
  max_temperature: "350.0"
  
  # Aumento térmico por tick mientras arde combustible
  heating_rate: "5.0"
  
  # Disipación térmica por tick cuando se extingue el fuego
  cooling_rate: "2.0"
  
  # Modificador de velocidad de combustión de sólidos
  fuel_burn_multiplier: "1.0"
```

---

## 4. Estados Dinámicos y Evaluación Reactiva (`states:`)

Croppy v0.6.0 introduce un sistema declarativo de **Estados de Estación** (`Station States`). Las estaciones de trabajo reaccionan dinámicamente a sus condiciones físicas y ambientales (como nivel térmico, combustible restante, progreso de cocción o ítems almacenados), alterando su modelo 3D, transformaciones espaciales (escala/posición/rotación), dimensiones de su caja de interacción física (hitbox) y emitiendo partículas y bucles de audio ambiental.

### Arquitectura Reactiva sin Persistencia Sintética
A diferencia de los cultivos que persisten etapas de crecimiento en el PDC del chunk, los estados de una estación se **evalúan de forma 100% reactiva en tiempo de ejecución** a partir de sus componentes físicos (`ThermalComponent`, `HeatSourceComponent`, `StorageComponent`, `ProcessComponent`). Al descargar o recargar chunks o tras reiniciar el servidor, el estado activo se recalcula al vuelo sin necesidad de almacenar campos de estado persistentes.

```
[Componentes Físicos: Temp / Combustible / Proceso / Almacenamiento]
                                │
                                ▼
                 StationStateEvaluator.evaluate()
                                │
                                ▼ (Resuelve por prioridad descendente)
                 [Definición del Estado Activo]
                 ├── Sobrescritura de Modelo (item_model, cmd, traslación, escala, rotación)
                 ├── Caja de Interacción Dinámica (width, height)
                 └── Efectos de Fase (partículas y bucle de audio ambiental)
                                │
                                ▼ (Paquetes Hot Metadata sin Despawn)
                 Sincronización Cliente con PacketEvents
```

### Expresiones Condicionales y Variables Disponibles
Las transiciones de estado se configuran mediante expresiones condicionales booleanas en el campo `when:` (o `condition:`). Durante cada tick, se inyectan las métricas físicas de la estación:

| Variable | Tipo | Descripción | Ejemplo de Condición |
|---|---|---|---|
| `temperature` | Double | Temperatura superficial en grados Celsius. | `temperature >= 100.0` |
| `temp` | Double | Alias de `temperature`. | `temp > 250.0` |
| `thermal_zone` | String | Zona termodinámica: `COLD`, `WARM`, `SIMMERING`, `BOILING`, `SEARING`, `BURNING`. | `thermal_zone == 'BOILING'` |
| `fuel_ticks` | Long | Ticks restantes de combustión de la fuente de calor subyacente. | `fuel_ticks > 0` |
| `is_active` | Boolean | True si la estación está caliente (> 25°C), procesando o tiene combustible. | `is_active == true` |
| `process_state` | String | Estado del proceso: `IDLE`, `RUNNING`, `AWAITING_INPUT`, `COMPLETED`, `PAUSED`. | `process_state == 'RUNNING'` |
| `has_inputs` | Boolean | True si la entrada contiene al menos una pila de ítems. | `has_inputs == true` |
| `input_count` | Integer | Cantidad total de pilas de ítems almacenadas en entradas. | `input_count >= 3` |
| `has_outputs` | Boolean | True si la salida contiene al menos un ítem cocinado. | `has_outputs == true` |

Los estados se evalúan en **orden descendente de prioridad**. El primer estado cuya condición se evalúa como `true` se activa inmediatamente. Si ninguna condición coincide, la estación adopta la configuración base predeterminada.

---

### Sobrescritura de Modelos (`states[].model`)

El bloque `model:` permite sobreescribir el modelo 3D base, CustomModelData y transformaciones espaciales:

```yaml
states:
  - id: "boiling"
    priority: 30
    when: "temperature >= 100.0 && process_state == 'RUNNING'"
    model:
      # Definición de modelo de ítem nativo de Minecraft 1.21.4+
      item_model: "croppy:station/cooking_pot/boiling"
      
      # Número entero para paquetes de recursos heredados basados en CustomModelData
      custom_model_data: 1005
      
      # Desplazamientos espaciales y escala relativos al centro del bloque
      translation: [0.0, 0.52, 0.0]
      scale: [1.02, 1.05, 1.02]
      rotation: [0.0, 45.0, 0.0]
```

* **Herencia de Coordenadas de Transformación**: Cualquier coordenada espacial omitida hereda automáticamente el valor de `visual.base_model`. Por ejemplo, especificar únicamente `scale: [1.1, 1.1, 1.1]` escala el modelo conservando la traslación y rotación base.
* **Estados Solo de Transformación**: Un estado puede definir únicamente `scale`, `translation` o `rotation` sin declarar un `item_model`, permitiendo animaciones de tamaño u orientación sobre el mismo modelo base.

---

### Cajas de Interacción Dinámicas (`states[].hitbox`)

El bloque `hitbox:` modifica las dimensiones de la entidad virtual `INTERACTION` en el cliente para adaptarse a la silueta activa:

```yaml
states:
  - id: "boiling"
    hitbox:
      width: 1.05
      height: 1.10
```

* **Formatos Flexibles**:
  * Formato mapa: `hitbox: { width: 1.05, height: 1.10 }` (dimensiones omitidas heredan de `visual.interaction`).
  * Formato lista: `hitbox: [1.05, 1.10]`.
  * Escalar uniforme: `hitbox: 1.05` (se aplica simultáneamente al ancho y alto).
* **Actualización en Caliente sin Despawn**: PacketEvents transmite paquetes de metadatos (`INDEX_INTERACTION_WIDTH` y `INDEX_INTERACTION_HEIGHT`) actualizando el área de clic sin destruir la entidad.

---

### Efectos Sensoriales de Ambiente (`states[].effects`)

Los estados activos pueden emitir partículas y bucles de sonido periódicos:

```yaml
states:
  - id: "boiling"
    effects:
      particle:
        type: "SPLASH"
        count: 3
        offset: [0.15, 0.45, 0.15]
        speed: 0.02
      sound:
        sound: "block.bubble_column.upwards_inside"
        interval_ticks: 30
        volume: 0.6
        pitch: 1.2
```

* **Partículas**: Nombres del enum `Particle` de Bukkit (ej. `CAMPFIRE_COSY_SMOKE`, `SPLASH`, `SMOKE`, `FLAME`).
* **Bucles de Sonido**: Reproduce el evento de sonido de Minecraft en el intervalo configurado (ej. `interval_ticks: 30` = cada 1.5 segundos).

---

## 5. Tabla de Referencia de Campos

| Ruta del Campo | Tipo | Predeterminado | Descripción |
|---|---|---|---|
| `id` | String (`Key`) | **Requerido** | Identificador único (ej. `croppy:cooking_pot`). |
| `name` | String | Valor de Key | Título formateado con soporte MiniMessage. |
| `block` | String | `minecraft:barrel` | Material físico colocado en el mundo. |
| `rotation_mode` | Enum | `CARDINAL_4` | Modo de orientación al colocar: `CARDINAL_4`, `CARDINAL_8` o `FREE_YAW`. Alias: `rotation`. |
| `interaction` | Enum | `HYBRID` | Modo de interacción: `PHYSICAL` (clics en mano), `GUI` (menú), o `HYBRID` (ambos). |
| `input_capacity` | Entero | `1` | Límite de pilas de ingredientes de entrada permitidos. |
| `output_capacity`| Entero | `1` | Límite de pilas de platos o residuos de salida. |
| `menu` | String (`Key`) | `croppy:default_station` | Clave del layout de interfaz en `menus/`. |
| `has_display` | Booleano | `true` | Interruptor para el renderizado de entidades virtuales. |
| `visual.hide_base_block` | Booleano | `false` | Enmascara el bloque físico en el mundo con paquetes de barrera para evitar interferencia visual con modelos 3D. |
| `visual.interaction.width` | Float | `0.8` | Ancho base de la caja de colisión para la entidad virtual `INTERACTION`. |
| `visual.interaction.height` | Float | `0.25` | Alto base de la caja de colisión para la entidad virtual `INTERACTION`. |
| `visual.base_model.enabled` | Booleano | `true` | Activa el display 3D base del recipiente. |
| `visual.base_model.type` | String | `ITEM` | Tipo de entidad de display: `ITEM` o `BLOCK`. |
| `visual.base_model.item` | String (`Key`) | `null` | Clave del ítem del Item Display base (ej. `croppy:cooking_pot`). |
| `visual.base_model.translation` | Float Array | `[0, 0, 0]` | Traslación base `[x, y, z]` relativa al centro del bloque. |
| `visual.base_model.scale` | Float Array | `[1, 1, 1]` | Factor de escala base `[x, y, z]`. |
| `visual.base_model.rotation` | Float Array | `[0, 0, 0]` | Ángulos de Euler base en grados `[pitch, yaw, roll]`. |
| `visual.base_model.billboard` | Enum | `FIXED` | Modo de billboard: `FIXED`, `CENTER`, `VERTICAL`, `HORIZONTAL`. |
| `visual.base_model.view_distance` | Entero | `48` | Distancia de renderizado en bloques para el modelo base. |
| `visual.process_display.enabled` | Booleano | `false` | Activa el ItemDisplay 3D para ingredientes en cocción. |
| `visual.process_display.translation` | Float Array | `[0, 0, 0]` | Vector de desplazamiento local `[x, y, z]` relativo al bloque. |
| `visual.process_display.scale` | Float Array | `[1, 1, 1]` | Vector de escala del modelo `[x, y, z]`. |
| `visual.process_display.view_distance` | Entero | `24` | Distancia máxima de visibilidad para ingredientes en proceso. |
| `visual.text_display.enabled` | Booleano | `false` | Activa el holograma flotante billboard. |
| `visual.text_display.translation` | Float Array | `[0, 1.2, 0]` | Altura del holograma sobre la estación. |
| `visual.text_display.billboard` | Enum | `CENTER` | Comportamiento de rotación: `CENTER` (mira al jugador), `FIXED`, `VERTICAL`, `HORIZONTAL`. |
| `visual.text_display.line_width` | Entero | `200` | Ancho máximo de línea antes del ajuste de línea en el cliente. |
| `visual.text_display.hide_when_idle` | Booleano | `false` | Oculta dinámicamente el holograma cuando la estación está fría (<= 25°C), vacía e inactiva. |
| `visual.text_display.format` | String | — | Plantilla MiniMessage multilínea con placeholders dinámicos. |
| `states` | Lista | `[]` | Lista de estados declarativos evaluados por prioridad. |
| `states[].id` | String | **Requerido** | Identificador único del estado (ej. `boiling`, `lit`, `idle`). |
| `states[].priority` | Entero | `0` | Prioridad de evaluación (valores mayores se evalúan primero). |
| `states[].when` | String | `null` | Expresión booleana condicional AST (alias: `condition`). |
| `states[].model.item_model` | String (`Key`) | `null` | Definición namespaced del modelo de ítem de Minecraft 1.21.4+. |
| `states[].model.custom_model_data` | Entero | `0` | Número de CustomModelData (0 si está desactivado). |
| `states[].model.translation` | Float Array | `base` | Desplazamiento espacial `[x, y, z]` sobreescrito. |
| `states[].model.scale` | Float Array | `base` | Escala espacial `[x, y, z]` o escalar uniforme sobreescrito. |
| `states[].model.rotation` | Float Array | `base` | Ángulos de Euler en grados `[pitch, yaw, roll]` sobreescritos. |
| `states[].hitbox.width` | Float | `base` | Ancho de la caja de colisión de interacción sobreescrito. |
| `states[].hitbox.height` | Float | `base` | Alto de la caja de colisión de interacción sobreescrito. |
| `states[].effects.particle.type` | String | `null` | Nombre del enum `Particle` de Bukkit. |
| `states[].effects.particle.count` | Entero | `2` | Cantidad de partículas emitidas por tick. |
| `states[].effects.particle.offset` | Float Array | `[0.15, 0.05, 0.15]` | Dispersión de emisión `[x, y, z]`. |
| `states[].effects.particle.speed` | Float | `0.02` | Velocidad de movimiento de partículas. |
| `states[].effects.sound.sound` | String | `null` | Clave del sonido de Minecraft (ej. `block.fire.ambient`). |
| `states[].effects.sound.interval_ticks` | Entero | `40` | Intervalo en ticks entre repeticiones del sonido. |
| `states[].effects.sound.volume` | Float | `1.0` | Volumen del sonido emitido. |
| `states[].effects.sound.pitch` | Float | `1.0` | Tono del sonido emitido. |

---

## 6. Placeholders Dinámicos del Text Display

La plantilla `visual.text_display.format` evalúa los siguientes valores dinámicos en cada tick de la simulación:

| Placeholder | Valor Reemplazado | Salida de Ejemplo |
|---|---|---|
| `{recipe_name}` | Nombre de la receta activa (o `Idle` por defecto) | `Sopa de Tomate` |
| `{recipe_display}` | Nombre de visualización formateado de la receta | `<yellow>Sopa de Tomate</yellow>` |
| `{progress_bar}` | Barra visual de progreso coloreada | `■■■■■□□□□□` |
| `{progress_percent}` | Porcentaje numérico de avance (`0-100`) | `65` |
| `{progress_percent_left}` | Porcentaje numérico restante (`100-0`) | `35` |
| `{progress_seconds}` | Segundos enteros transcurridos de cocción | `12` |
| `{progress_seconds_left}` | Segundos enteros restantes de cuenta regresiva | `8` |
| `{progress_ticks}` | Duración transcurrida del proceso en ticks | `240` |
| `{progress_ticks_left}` | Duración restante del proceso en ticks | `160` |
| `{time_remaining}` | Cadena de tiempo restante amigable para humanos | `8s` / `1m 20s` |
| `{temperature_c}` | Temperatura en grados Celsius con símbolo | `115.4°C` |
| `{temperature}` | Temperatura numérica en Celsius sin sufijo de unidad | `115.4` |
| `{thermal_zone}` | Clasificación de zona termodinámica | `SIMMERING` (NONE, WARM, MEDIUM, BOILING, SCORCHING) |
| `{phase_name}` | Nombre de la fase secuencial activa de cocción | `Dorar` |
| `{phase_index}` | Índice basado en 1 de la fase actual | `1` |
| `{phase_total}` | Cantidad total de fases de la receta | `3` |
| `{status_message}` | Mensaje diagnóstico contextual o mensaje de error | `Falta Fuente de Calor` |
| `{state}` | Estado global de la estación | `COOKING`, `IDLE`, `HEATING`, `PAUSED`, `COMPLETED`, `FAILED`, `OVERCOOKING` |

---

## 7. Modos de Interacción

* **`PHYSICAL`**:
  * Clic derecho con un ingrediente lo inserta directamente en el recipiente.
  * Clic derecho agachado con la mano vacía extrae el plato cocinado o ingrediente superior.
  * No se abre ninguna interfaz de inventario. Ideal para sartenes, parrillas y tablas de cortar.
* **`GUI`**:
  * El clic derecho abre la interfaz contenedora 9xN asociada.
  * Los clics físicos en el bloque no manipulan ítems directamente.
* **`HYBRID`**:
  * El clic derecho normal inserta ítems físicamente para agilidad en cocina.
  * El clic derecho agachado abre el menú de inventario para inspección completa y control detallado.

### Apilamiento Limpio de Ítems Vanilla (Stackability)
Al extraer ingredientes del almacenamiento de una estación (sea mediante extracción física o a través de la interfaz GUI), Croppy distingue entre ítems custom registrados e ingredientes nativos de Minecraft. Los ingredientes vanilla se devuelven limpios, sin nombres de visualización sintéticos ni etiquetas PDC de Croppy, garantizando que se apilen de forma nativa (`ItemStack#isSimilar() == true`) con las pilas existentes en el inventario del jugador.
