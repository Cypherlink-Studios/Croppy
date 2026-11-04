---
title: Configuración de Recetas Culinarias
description: Especificación técnica y guía de configuración YAML para recetas culinarias, matrices de ingredientes, procesos y condiciones de descubrimiento.
sidebar:
  order: 13
---

# Configuración de Recetas Culinarias

Las recetas culinarias en Croppy definen cómo los cultivos agrícolas, las especias y los ingredientes base se combinan dentro de estaciones de cocina físicas para elaborar platos y manjares. Las recetas se almacenan como archivos modulares YAML en el directorio `plugins/Croppy/recipes/`.

El motor analiza estas definiciones mediante `YamlRecipeLoader`, registrándolas en el `RecipeRegistry` para el procesamiento en estaciones, coincidencia anti-solapamiento (*anti-shadowing*) y seguimiento de descubrimiento de jugadores.

---

## 1. Desglose Completo del Esquema YAML

A continuación se muestra un ejemplo completo correspondiente a `plugins/Croppy/recipes/tomato_soup.yml`:

```yaml
# Clave semántica única con namespace (<namespace>:<valor>)
id: "croppy:tomato_soup"

# Nombre para mostrar con formato MiniMessage
name: "<gradient:#ff5555:#ffaa00>Sopa de Tomate</gradient>"

# Clave de categoría para filtrado en el menú del Compendio Culinario
# Predeterminado: "croppy:all" si se omite
category: "croppy:soups"

# Estación de trabajo física requerida para preparar esta receta
station: "croppy:cooking_pot"

# ID del proceso secuencial que rastrea los estados de cocción
# Predeterminado: toma el mismo valor que 'id' si se omite
process: "croppy:tomato_soup_cook"

# Lista de ingredientes de entrada requeridos
inputs:
  - item: "croppy:tomato"
    amount: 2
  - item: "minecraft:bowl"
    amount: 1

# Plato producido cuando la cocción se completa con éxito
result:
  item: "croppy:tomato_soup"
  amount: 1

# Configuración del motor de descubrimiento y progresión
discovery:
  # Indica si la estación tolera ingredientes extra no requeridos
  allow_excess_inputs: false
  
  # Métodos de descubrimiento habilitados para desbloquear esta receta
  methods:
    - "croppy:experimental"  # Se desbloquea cocinando a ciegas con éxito
    - "croppy:automatic"     # Se desbloquea al obtener los ingredientes clave

  # Pista culinaria presentada cuando la receta está en estado HINTED
  hint:
    description: "<gray>Una cálida sopa roja cocinada a fuego lento con tomates maduros de huerto y servida en un cuenco.</gray>"
    revealed_ingredients:
      - "croppy:tomato"
    hide_result: true

# Propiedades y metadatos adicionales
properties:
  difficulty: "easy"
  simmer_temperature: "95.0"
```

---

## 2. Tabla de Referencia de Campos

| Ruta del Campo | Tipo | Requerido | Predeterminado | Descripción |
|---|---|---|---|---|
| `id` | String (`Key`) | **Sí** | — | Identificador único en formato `<namespace>:<valor>` (ej. `croppy:tomato_soup`). |
| `name` | String | **Sí** | — | Nombre visible para el jugador con soporte para etiquetas MiniMessage. |
| `category` | String (`Key`) | No | `croppy:all` | Clave de categoría para pestañas de navegación en el compendio. |
| `station` | String (`Key`) | **Sí** | — | Clave de la estación donde se cocina (`croppy:cooking_pot`, `croppy:skillet`). |
| `process` | String (`Key`) | No | Mismo que `id`| Identificador del [Proceso Secuencial de Cocción](cooking-processes.md) que gobierna tiempos, fases, efectos y hologramas. |
| `inputs` | Lista de Mapas | **Sí** | `[]` | Ingredientes requeridos. Cada entrada requiere `item` (Key) y `amount` opcional (entero, defecto `1`). |
| `result.item` | String (`Key`) | No | Mismo que `id`| Identificador del ítem producido (ej. `croppy:tomato_soup`). |
| `result.amount` | Entero | No | `1` | Cantidad de ítems generados al terminar la preparación. |
| `discovery.allow_excess_inputs` | Booleano | No | `false` | Si es `false`, la cocción se cancela si hay ítems extraños en la estación. |
| `discovery.methods` | Lista de Strings | No | `[]` | Métodos de descubrimiento SPI (`croppy:experimental`, `croppy:automatic`). |
| `discovery.hint.description` | String | No | Pista por defecto | Texto de lore en MiniMessage mostrado en el compendio en estado `HINTED`. |
| `discovery.hint.revealed_ingredients`| Lista de Strings | No | `[]` | Ingredientes visibles en el compendio mientras está en estado `HINTED`. |
| `discovery.hint.hide_result` | Booleano | No | `true` | Si es `true`, el icono y nombre del plato final permanecen ocultos en `HINTED`. |
| `properties` | Map<String, String> | No | `{}` | Metadatos arbitrarios (ej. dificultad, temperatura recomendada). |

---

## 3. Mecánicas de Descubrimiento y Pistas

Croppy incorpora un sistema de progresión culinaria configurado mediante el bloque `discovery`:

```text
+-----------------------------------------------------------------------------+
|                     PIPELINE DE PROGRESIÓN DE RECETAS                       |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [UNKNOWN]     --> El compendio muestra un ícono de misterio ("???").       |
|       |                                                                     |
|       v                                                                     |
|  [HINTED]      --> Muestra 'hint.description' e ingredientes revelados.     |
|       |            El plato final permanece oculto si 'hide_result: true'.  |
|       v                                                                     |
|  [DISCOVERED]  --> Receta completa visible: ingredientes, cantidades y olla.|
|                                                                             |
+-----------------------------------------------------------------------------+
```

### Métodos de Descubrimiento
* `croppy:experimental`: Permite el descubrimiento por ensayo y error. Si un cocinero coloca los ingredientes válidos en una estación caliente y completa la cocción, la receta se desbloquea de forma permanente.
* `croppy:automatic`: Desbloquea la receta tan pronto como el jugador recolecta o sostiene los ingredientes principales en su inventario.

---

## 4. Tutorial Paso a Paso: Creación de una Receta Personalizada

Crearemos una nueva receta para la sartén: **Tortilla de Zanahoria** (`croppy:carrot_omelette`).

### Paso 1: Crear el archivo YAML
Crea el archivo `plugins/Croppy/recipes/carrot_omelette.yml`:

```yaml
id: "croppy:carrot_omelette"
name: "<gradient:#ffaa00:#ffff55>Tortilla de Zanahoria</gradient>"
category: "croppy:fried"
station: "croppy:skillet"

inputs:
  - item: "minecraft:egg"
    amount: 1
  - item: "minecraft:carrot"
    amount: 2

result:
  item: "croppy:carrot_omelette"
  amount: 1

discovery:
  allow_excess_inputs: false
  methods:
    - "croppy:experimental"
  hint:
    description: "<gray>Bate huevos frescos con zanahorias dulces picadas a fuego vivo.</gray>"
    revealed_ingredients:
      - "minecraft:carrot"
    hide_result: false

properties:
  difficulty: "medium"
```

### Paso 2: Verificar la existencia del ítem de resultado
Asegúrate de que `croppy:carrot_omelette` esté registrado en `plugins/Croppy/items/ingredients/carrot_omelette.yml` (o haz referencia a un ítem vanilla como `minecraft:cooked_chicken`).

### Paso 3: Recargar la configuración
Ejecuta en el juego o consola:
```text
/croppy reload
```
El servidor registrará en los logs:
```text
[Croppy] Registered 4 recipe definitions into RecipeRegistry.
```

¡Tu nueva receta ya está operativa y lista para ser descubierta o consultada en el libro mediante `/croppy recipes all`!
