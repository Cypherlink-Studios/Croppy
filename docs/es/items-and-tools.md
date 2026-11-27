---
title: Ítems y Herramientas
description: Catálogo de ítems custom de Croppy, herramientas agrícolas, persistencia de datos en PDC y formato de registro.
sidebar:
  order: 18
---

# Ítems Personalizados y Herramientas Agrícolas

Croppy proporciona un marco unificado que amplía los ítems vanilla con lógica y comportamientos propios. Todos los ítems se identifican mediante claves semánticas (`croppy:<nombre>`), admiten el componente moderno Data Component `item_model` de Minecraft 1.21.4+, valores enteros de `CustomModelData` para packs de texturas clásicos, y almacenan datos inmutables y mutables en el **PersistentDataContainer (PDC)** nativo de Minecraft.

---

## 1. Catálogo de Herramientas Agrícolas y de Cocina

Croppy equipa a granjeros y cocineros con implementos especializados:

```text
+-----------------------------------------------------------------------------+
|                     HERRAMIENTAS AGRÍCOLAS Y DE COCINA                      |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [INSPECTOR DE SUELO Y CULTIVOS] (croppy:inspector)                         |
|  - Material Base: SPYGLASS                                                  |
|  - Función: Clic derecho sobre tierra arada o cultivos para ver el informe  |
|    en chat (especie, etapa, progreso %, luz, humedad, nutrientes NPK).      |
|                                                                             |
|  [TERMÓMETRO CULINARIO] (croppy:thermometer)                                |
|  - Material Base: BLAZE_ROD                                                 |
|  - Función: Clic derecho sobre una estación de cocina o estufa para leer su |
|    temperatura exacta en grados Celsius y su zona térmica.                  |
|                                                                             |
|  [LIBRO DE RECETAS] (croppy:recipe_book)                                    |
|  - Material Base: BOOK                                                      |
|  - Función: Clic derecho en la mano para abrir de forma instantánea la      |
|    interfaz interactiva del Compendio Culinario.                            |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Categorías de Ítems

Los ítems se organizan en subcarpetas temáticas bajo `plugins/Croppy/items/<categoría>/`:

| Categoría | Contenido Habitual | Ejemplos |
|---|---|---|
| `seeds` | Semillas de cultivos vinculadas a definiciones de especies | `croppy:wheat_seeds`, `croppy:tomato_seeds` |
| `tools` | Herramientas operativas y de diagnóstico | `croppy:inspector`, `croppy:thermometer` |
| `watering_cans` | Regaderas con capacidades y áreas de riego progresivas | `croppy:copper_can`, `croppy:gold_can` |
| `fertilizers` | Enmiendas y abonos (Crecimiento, Calidad, Retención) | `croppy:growth_fertilizer`, `croppy:hydrating_fertilizer` |
| `stations` | Bloques físicos de estaciones de trabajo | `croppy:cooking_pot`, `croppy:skillet`, `croppy:stove` |
| `ingredients` | Cosechas, carnes preparadas, caldos y platos culinarios | `croppy:tomato`, `croppy:tomato_soup` |
| `misc` | Libros de cocina, manuales y objetos misceláneos | `croppy:recipe_book`, `croppy:burnt_food` |

---

## 3. Especificación del Esquema YAML de Ítems

Cada archivo de configuración es analizado por `YamlItemLoader`. A continuación se muestra un ejemplo de referencia completo:

```yaml
id: "croppy:growth_fertilizer"
category: "fertilizers"

# Título visible con formato MiniMessage y degradado
name: "<gradient:#55ff55:#22aa22>Fertilizante Speed-Gro</gradient>"

# Líneas de lore en el tooltip del ítem
lore:
  - "<gray>Enriquece la tierra con nitratos activos.</gray>"
  - "<gold>+25% Velocidad de Crecimiento</gold>"

# Material base de Minecraft
material: "BONE_MEAL"

# Componente nativo de Minecraft 1.21.4+ (assets/<ns>/items/*.json)
item_model: "croppy:growth_fertilizer"

# Entero CustomModelData para compatibilidad clásica
custom_model_data: 1001

# Textura custom para cabezas de jugador (Base64 o URL directa de Mojang)
# texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv..."
# Alias: skull-texture

# Atributos de durabilidad y visibilidad
unbreakable: true
glow: false
flags:
  - "HIDE_ATTRIBUTES"
  - "HIDE_ADDITIONAL_TOOLTIP"

# Etiquetas personalizadas escritas en el PersistentDataContainer
pdc:
  type: "SPEED"
  speed_multiplier: "1.25"
```

### Tabla de Referencia de Campos

| Ruta del Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `id` | String (`Key`) | **Sí** | Identificador único en formato `<namespace>:<valor>`. |
| `category` | String | **Sí** | Subcarpeta de categoría (ej. `seeds`, `tools`, `ingredients`). |
| `name` | String | **Sí** | Nombre visible del ítem con soporte MiniMessage. |
| `material` | String | **Sí** | Nombre del Material Bukkit/Minecraft (ej. `BONE_MEAL`, `SPYGLASS`). |
| `lore` | Lista de Strings | No | Descripción multilínea con formato MiniMessage. |
| `item_model` | String (`Key`)| No | Ruta del Data Component nativo `item_model` para 1.21.4+. |
| `custom_model_data` | Entero | No | Valor numérico entero de CustomModelData. |
| `texture` | String | No | Textura de skin personalizada para ítems `PLAYER_HEAD`. Admite Base64 o URL directa de Mojang (`http://textures.minecraft.net/texture/...`). Alias: `skull-texture`. |
| `unbreakable` | Booleano | No | Marca la herramienta como irrompible. |
| `glow` | Booleano | No | Aplica brillo de encantamiento sin mostrar encantamientos. |
| `flags` | Lista de Strings | No | ItemFlags para ocultar tooltips vanilla (ej. `HIDE_ATTRIBUTES`). |
| `pdc` | Map<String, String> | No | Metadatos persistentes String guardados en el `PersistentDataContainer`. |

---

## 4. Texturas Personalizadas de Cabezas (Base64 y URLs de Mojang)

Los ítems con `material: PLAYER_HEAD` (o `minecraft:player_head`) pueden definir texturas de skin personalizadas mediante la propiedad `texture` (o su alias `skull-texture`).

Croppy resuelve automáticamente tanto URLs directas de texturas de Mojang como cargas útiles JSON codificadas en Base64:

### Ejemplo con Valor Base64
```yaml
id: "croppy:cutting_board"
category: "stations"
name: "<yellow>Tabla de Cortar</yellow>"
material: "PLAYER_HEAD"
texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJkYTVmMjc3YzhlMmM2NWVkN2JhOTJkMWIxY2M5YTc5YTRlOTQ1OWVkNWU0ZTk0YjE4ZmVhYTk3N2FjIn19fQ=="
```

### Ejemplo con URL Directa de Mojang
```yaml
id: "croppy:cutting_board"
category: "stations"
name: "<yellow>Tabla de Cortar</yellow>"
material: "PLAYER_HEAD"
texture: "http://textures.minecraft.net/texture/4bda5f277c8e2c65ed7ba92d1b1cc9a79a4e9459ed5e4e94b18feaa977ac"
```

### Generación Determinista de Perfiles (UUID v3)
Para evitar latencia de red y colisiones de skins en Paper, Croppy genera un `PlayerProfile` con un UUID determinista derivado del hash de la textura (MD5). Cuando el ítem se coloca físicamente en el mundo como bloque base de una estación, el estado del tile entity `Skull` del bloque se actualiza de inmediato con dicho perfil.

---

## 5. Arquitectura del PersistentDataContainer (PDC)

Cada ítem de Croppy almacena etiquetas seguras en su NBT/PDC:
* **`croppy:item_id`**: Identificador semántico inmutable que vincula el ítem con su definición en el registro.
* **`croppy:category`**: Categoría de clasificación semántica del ítem.
* **`croppy:water_level` / `croppy:capacity`**: Almacena las cargas de agua disponibles en las regaderas.
* **`croppy:fertilizer_type` / `croppy:speed_multiplier`**: Registra propiedades de fertilización en semillas y tierra tratada.

Al residir directamente en la estructura nativa de Minecraft, estos datos sobreviven a reinicios del servidor, traslados entre cofres e interacciones con otros plugins sin desincronizarse.

---

## 6. Catálogo Interactivo de Ítems y Spawner Administrativo

Los administradores pueden explorar, filtrar, buscar y obtener cualquier ítem registrado directamente mediante el menú interactivo `/croppy items`.

```text
/croppy items [categoría | search | clear]
```

### Características Principales
* **Filtrado por Pestañas de Categoría**: Alterna entre las categorías predeterminadas (`seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`, `all`) o categorías personalizadas registradas por addons.
* **Paginación Horizontal de Categorías**: Si múltiples addons registran categorías adicionales que superan las ranuras disponibles en la fila superior, los botones de navegación horizontal permiten desplazarse entre páginas de pestañas.
* **Búsqueda en Tiempo Real por Chat**: Clic en el botón de búsqueda o uso de `/croppy items search` para filtrar ítems al instante por nombre, material o clave con namespace.
* **Controles Rápidos del Spawner**:
  * **Clic Izquierdo**: Entrega 1 unidad del ítem al inventario.
  * **Clic Derecho**: Entrega 1 stack completo (64x o el máximo del ítem).
  * **Shift + Clic**: Sugiere el comando de obtención en el chat `/croppy item give <categoría> <id> 1`.
  * **Tecla Q (Drop)**: Suelta 1 unidad directamente a los pies del jugador.
  * **Control + Q**: Suelta 1 stack completo a los pies del jugador.
  * **Caída Natural por Inventario Lleno**: Si el inventario del jugador está completo, los ítems sobrantes caen al suelo a sus pies sin perderse.

---

## 7. Atributos de Consumibles, Efectos Custom y Alcohol

Los ítems personalizados pueden declarar física nativa de comida y consumo de Paper 1.21.8 junto con efectos instantáneos y temporales ejecutados en servidor dentro de `items/<categoría>/<item_id>.yml`:

```yaml
id: "croppy:spiced_cider"
category: "ingredients"
material: "POTION"
item-model: "croppy:spiced_cider"
name: "<gold>Sidra Especiada</gold>"

food:                          # Alias: nutrition
  hunger: 4
  saturation: 3.5
  can-always-eat: true
  quality-scaling:
    hunger-per-level: 0.25
    saturation-per-level: 0.25
    duration-per-level: 0.20

consumable:
  consume-seconds: 1.2
  animation: "DRINK"           # EAT, DRINK, NONE, BLOCK, BOW, SPEAR, CROSSBOW, SPYGLASS, TOOT_HORN, BRUSH, BUNDLE
  sound: "minecraft:entity.generic.drink"
  particles: true
  remainder: "minecraft:glass_bottle"
  cooldown:
    seconds: 3.0
    group: "croppy:cider"

effects:
  - type: "croppy:potion"
    effect: "minecraft:regeneration"
    duration: "5s"
    amplifier: 0
    min-quality: 1
    chance: 1.0
    condition: "item.quality >= 1"
  - type: "croppy:alcohol"
    abv: 5.5
    volume-ml: 250
```

### Handlers de Efectos Instantáneos Incluidos
| Clave | Parámetros Principales | Comportamiento |
|---|---|---|
| `croppy:potion` | `effect`, `duration`, `amplifier`, `ambient`, `particles`, `icon` | Aplica un `PotionEffect` vanilla (duración escalada por `quality-scaling`). |
| `croppy:remove_potion` | `effect` o `effects` | Elimina los efectos de poción vanilla indicados. |
| `croppy:clear_effects` | `vanilla` (`true`), `custom` (`false`) | Limpia efectos vanilla y/o efectos de estado custom de Croppy. |
| `croppy:heal` / `croppy:damage` | `amount` | Cura (hasta la vida máxima) o daña al jugador. |
| `croppy:oxygen` | `amount` (ticks) | Ajusta el aire restante del jugador. |
| `croppy:sound` / `croppy:particle` | `sound`/`particle`, `volume`, `pitch`, `count` | Reproduce un sonido o genera partículas sobre el jugador. |
| `croppy:message` | `message`, `target` (`CHAT` \| `ACTIONBAR`) | Envía un mensaje formateado con MiniMessage. |
| `croppy:teleport_randomly` | `diameter`, `attempts` | Teletransporta de forma segura a un bloque cercano. |
| `croppy:apply_status` | `status`, `duration`, `amplifier`, `intensity` | Aplica un `CustomStatusEffect` temporal con estado. |
| `croppy:reduce_status` / `croppy:remove_status` | `status`, `intensity`, `duration` | Reduce intensidad/duración o elimina un efecto activo (ideal para antídotos como café). |
| `croppy:alcohol` | `abv`, `volume-ml`, `factor` (o `amount`) | Suma intensidad a `croppy:intoxication` (escala `0.0..100.0`). |

### Intoxicación Alcohólica (`croppy:intoxication`) y Resaca (`croppy:hangover`)
`croppy:intoxication` decae de forma continua con el tiempo y progresa por etapas configurables (`Tipsy` >= 20, `Drunk` >= 45, `Heavily Drunk` >= 70, `Blackout` >= 90) con pulsos cortos de náuseas, buffs/debuffs por etapa, slurring/balanceo *opt-in*, reducción parcial con leche y transición opcional a `croppy:hangover` al recuperarse de una intoxicación severa.

