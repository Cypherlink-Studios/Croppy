---
title: Ítems y Herramientas
description: Catálogo de ítems custom de Croppy, herramientas agrícolas, persistencia de datos en PDC y formato de registro.
sidebar:
  order: 17
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
| `fertilizers` | Enmiendas y abonos (Crecimiento, Calidad, Retención) | `croppy:speed_gro`, `croppy:retaining_soil` |
| `stations` | Bloques físicos de estaciones de trabajo | `croppy:cooking_pot`, `croppy:skillet`, `croppy:stove` |
| `ingredients` | Cosechas, carnes preparadas, caldos y platos culinarios | `croppy:tomato`, `croppy:tomato_soup` |
| `misc` | Libros de cocina, manuales y objetos misceláneos | `croppy:recipe_book`, `croppy:burnt_food` |

---

## 3. Especificación del Esquema YAML de Ítems

Cada archivo de configuración es analizado por `YamlItemLoader`. A continuación se muestra un ejemplo de referencia completo:

```yaml
id: "croppy:speed_gro"
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
item_model: "croppy:speed_gro"

# Entero CustomModelData para compatibilidad clásica
custom_model_data: 1001

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
| `unbreakable` | Booleano | No | Marca la herramienta como irrompible. |
| `glow` | Booleano | No | Aplica brillo de encantamiento sin mostrar encantamientos. |
| `flags` | Lista de Strings | No | ItemFlags para ocultar tooltips vanilla (ej. `HIDE_ATTRIBUTES`). |
| `pdc` | Map<String, String> | No | Metadatos persistentes String guardados en el `PersistentDataContainer`. |

---

## 4. Arquitectura del PersistentDataContainer (PDC)

Cada ítem de Croppy almacena etiquetas seguras en su NBT/PDC:
* **`croppy:item_id`**: Identificador semántico inmutable que vincula el ítem con su definición en el registro.
* **`croppy:category`**: Categoría de clasificación semántica del ítem.
* **`croppy:water_level` / `croppy:capacity`**: Almacena las cargas de agua disponibles en las regaderas.
* **`croppy:fertilizer_type` / `croppy:speed_multiplier`**: Registra propiedades de fertilización en semillas y tierra tratada.

Al residir directamente en la estructura nativa de Minecraft, estos datos sobreviven a reinicios del servidor, traslados entre cofres e interacciones con otros plugins sin desincronizarse.
