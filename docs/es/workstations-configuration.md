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

A continuación se presenta un ejemplo exhaustivo correspondiente a `plugins/Croppy/stations/cooking_pot.yml`:

```yaml
# Clave semántica única con namespace de la estación
id: "croppy:cooking_pot"

# Nombre para mostrar con formato de color MiniMessage
name: "<gradient:#ffaa00:#ff5555>Olla de Cocina</gradient>"

# Bloque físico de Minecraft que representa esta estación en el mundo
block: "minecraft:player_head"

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
  # Display 3D de Ítems: Renderiza los ingredientes flotando dentro de la olla
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

## 4. Tabla de Referencia de Campos

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
| `visual.process_display.enabled` | Booleano | `false` | Activa el ItemDisplay 3D para ingredientes. |
| `visual.process_display.translation` | Float Array | `[0, 0, 0]` | Vector de desplazamiento local `[x, y, z]` relativo al bloque. |
| `visual.process_display.scale` | Float Array | `[1, 1, 1]` | Vector de escala del modelo `[x, y, z]`. |
| `visual.process_display.view_distance` | Entero | `16` | Distancia máxima de visibilidad para los paquetes cliente. |
| `visual.text_display.enabled` | Booleano | `false` | Activa el holograma flotante billboard. |
| `visual.text_display.translation` | Float Array | `[0, 1.2, 0]` | Altura del holograma sobre la estación. |
| `visual.text_display.billboard` | Enum | `CENTER` | Comportamiento de rotación: `CENTER` (mira al jugador), `FIXED`, `VERTICAL`, `HORIZONTAL`. |
| `visual.text_display.line_width` | Entero | `200` | Ancho máximo de línea antes del ajuste de línea en el cliente. |
| `visual.text_display.hide_when_idle` | Booleano | `false` | Oculta dinámicamente el holograma cuando la estación está fría (<= 25°C), vacía e inactiva para evitar ruido visual. |
| `visual.text_display.format` | String | — | Plantilla MiniMessage multilínea con placeholders dinámicos. |
| `visual.hide_base_block` | Booleano | `false` | Enmascara el bloque físico en el mundo con paquetes de barrera para evitar interferencia visual con modelos 3D. |
| `visual.interaction.width` | Float | `0.8` | Ancho de la caja de colisión para la entidad virtual `INTERACTION`. |
| `visual.interaction.height` | Float | `0.25` | Alto de la caja de colisión para la entidad virtual `INTERACTION`. |

---

## 5. Placeholders Dinámicos del Text Display

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

## 6. Modos de Interacción

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
