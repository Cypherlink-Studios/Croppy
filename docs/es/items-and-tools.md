---
title: Ítems y Herramientas
description: Catálogo de ítems custom de Croppy, herramientas agrícolas, persistencia de datos en PDC y formato de registro.
sidebar:
  order: 14
---

# Ítems Personalizados y Herramientas Agrícolas

Croppy proporciona un marco unificado que amplía los ítems vanilla con lógica y comportamientos propios. Todos los ítems se identifican mediante claves semánticas (`croppy:<nombre>`), admiten `CustomModelData` para packs de texturas y almacenan datos inmutables y mutables en el **PersistentDataContainer (PDC)** nativo de Minecraft.

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
| `seeds` | Semillas de cultivos vinculadas a definiciones de especies | `croppy:wheat_seeds`, `croppy:carrot_seeds` |
| `tools` | Herramientas operativas y de diagnóstico | `croppy:inspector`, `croppy:thermometer` |
| `watering_cans` | Regaderas con capacidades y áreas de riego progresivas | `croppy:copper_can`, `croppy:gold_can` |
| `fertilizers` | Enmiendas y abonos (Crecimiento, Calidad, Retención) | `croppy:speed_gro`, `croppy:retaining_soil` |
| `stations` | Bloques físicos de estaciones de trabajo | `croppy:cooking_pot`, `croppy:skillet`, `croppy:stove` |
| `misc` | Libros de cocina, manuales y objetos misceláneos | `croppy:recipe_book` |

---

## 3. Arquitectura del PersistentDataContainer (PDC)

Cada ítem de Croppy almacena etiquetas seguras en su NBT/PDC:
* **`croppy:item_id`**: Identificador semántico inmutable que vincula el ítem con su definición en el registro.
* **`croppy:water_level` / `croppy:capacity`**: Almacena las cargas de agua disponibles en las regaderas.
* **`croppy:fertilizer_type` / `croppy:bonus`**: Registra propiedades de fertilización en semillas y tierra tratada.

Al residir directamente en la estructura nativa de Minecraft, estos datos sobreviven a reinicios del servidor, traslados entre cofres e interacciones con otros plugins sin desincronizarse.
