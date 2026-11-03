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

## 2. Estaciones Generadoras de Calor (`heat_source`)

Las estufas y hogares de combustión declaran curvas termodinámicas en lugar de recetas. A continuación se detalla la configuración de `plugins/Croppy/stations/stove.yml`:

```yaml
id: "croppy:stove"
name: "<gradient:#ff8800:#ff3300>Estufa Rústica</gradient>"
block: "minecraft:smoker"
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

## 3. Tabla de Referencia de Campos

| Ruta del Campo | Tipo | Predeterminado | Descripción |
|---|---|---|---|
| `id` | String (`Key`) | **Requerido** | Identificador único (ej. `croppy:cooking_pot`). |
| `name` | String | Valor de Key | Título formateado con soporte MiniMessage. |
| `block` | String | `minecraft:barrel` | Material físico colocado en el mundo. |
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
| `visual.text_display.format` | String | — | Plantilla MiniMessage multilínea con placeholders dinámicos. |

---

## 4. Placeholders Dinámicos del Text Display

La plantilla `visual.text_display.format` evalúa los siguientes valores dinámicos en cada tick de la simulación:

| Placeholder | Valor Reemplazado | Salida de Ejemplo |
|---|---|---|
| `{recipe_name}` | Nombre de la receta activa | `Sopa de Tomate` |
| `{progress_bar}` | Barra visual de progreso coloreada | `■■■■■□□□□□` |
| `{progress_percent}` | Porcentaje numérico de avance | `65` |
| `{temperature_c}` | Temperatura en grados Celsius | `115.4` |
| `{thermal_zone}` | Clasificación de zona termodinámica | `Simmering` (Warm, Medium, Boiling, Scorching) |
| `{state}` | Estado global de la estación | `COOKING`, `IDLE`, `OVERCOOKING` |

---

## 5. Modos de Interacción

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
