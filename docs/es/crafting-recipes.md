---
title: Recetas de Mesa de Crafteo
description: Sistema declarativo de recetas de crafteo Bukkit para estaciones, herramientas, regaderas e ítems personalizados en mesas de crafteo vanilla.
sidebar:
  order: 22
---

# Recetas de Mesa de Crafteo

Croppy proporciona un sistema declarativo de recetas de crafteo Bukkit que permite fabricar estaciones culinarias, herramientas agrícolas, regaderas por niveles y fertilizantes de forma nativa en mesas de crafteo estándar de Minecraft en modo Supervivencia.

Las recetas de crafteo se almacenan como archivos YAML modulares en `plugins/Croppy/crafting/` (independientes de las recetas culinarias ubicadas en `plugins/Croppy/recipes/`).

---

## 1. Recetas de Crafteo vs Recetas Culinarias

Es esencial distinguir entre los dos subsistemas de recetas en Croppy:

| Característica | Recetas de Crafteo (`crafting/`) | Recetas Culinarias (`recipes/`) |
|---|---|---|
| **Ubicación** | `plugins/Croppy/crafting/**/*.yml` | `plugins/Croppy/recipes/**/*.yml` |
| **Estación de Trabajo** | Mesa de crafteo vanilla de Minecraft (rejilla 3x3) | Estaciones físicas de Croppy (Olla, Sartén, Fogón) |
| **Sistema** | `ShapedRecipe` / `ShapelessRecipe` nativo de Bukkit | Motor de simulación culinaria multietapa propio |
| **Libro de Recetas** | Integrado en el Libro de Recetas vanilla de Minecraft | Registrado en la GUI del Compendio Culinario (`/croppy recipes`) |
| **Propósito** | Ensamblar utensilios, forjar regaderas, crear herramientas | Cocinar sopas, saltear carnes, hornear platos |

---

## 2. Tipos de Recetas y Esquema YAML

Los archivos de recetas soportan dos formatos principales: **`shaped`** (con forma fija) y **`shapeless`** (sin forma).

### 2.1 Ejemplo de Receta con Forma (`shaped`)

Una receta con forma exige una disposición espacial exacta en la cuadrícula (desde 1x1 hasta 3x3).

```yaml
# Identificador semántico con espacio de nombres
id: "croppy:cooking_pot"

# Tipo de receta: shaped
type: "shaped"

# Identificador opcional de grupo en el libro de recetas
group: "croppy_stations"

# Categoría opcional del libro de recetas: misc, equipment, building, redstone (por defecto: misc)
category: "misc"

# Ítem resultante (por defecto adopta el id con cantidad 1 si se omite)
result:
  item: "croppy:cooking_pot"
  amount: 1

# Matriz de patrón (1 a 3 filas, 1 a 3 caracteres por fila)
shape:
  - "I I"
  - "ICI"
  - " I "

# Asignación de caracteres a ingredientes
ingredients:
  I: "minecraft:iron_ingot"
  C: "minecraft:cauldron"
```

### 2.2 Ejemplo de Receta sin Forma (`shapeless`)

Una receta sin forma admite cualquier disposición de los ingredientes en la rejilla (hasta 9 ítems).

```yaml
id: "croppy:growth_fertilizer"
type: "shapeless"
group: "croppy_fertilizers"
category: "misc"

result:
  item: "croppy:growth_fertilizer"
  amount: 2

ingredients:
  - "minecraft:bone_meal"
  - "minecraft:rotten_flesh"
```

---

## 3. Resolución de Ingredientes

Croppy traduce automáticamente las declaraciones de ingredientes en instancias nativas de `RecipeChoice` de Bukkit:

### 1. Materiales Vanilla
* Formato: `"minecraft:nombre_material"` o `"nombre_material"` (ej. `"minecraft:iron_ingot"`, `"campfire"`).
* Se mapea a: Bukkit `RecipeChoice.MaterialChoice`.

### 2. Etiquetas (Tags) Vanilla
* Formato: `"#minecraft:nombre_tag"` o `"#nombre_tag"` (ej. `"#minecraft:planks"`, `"#wool"`).
* Se mapea a: Bukkit `RecipeChoice.MaterialChoice` aceptando cualquier material perteneciente a la etiqueta.

### 3. Ítems Personalizados de Croppy (ExactChoice con Preservación de PDC)
* Formato: `"croppy:id_item"` (ej. `"croppy:wooden_can"`, `"croppy:cooking_pot"`).
* Se mapea a: Bukkit `RecipeChoice.ExactChoice` construido a través de `ItemFactory`.
* **Seguridad y Precisión**: La coincidencia exacta valida etiquetas en el PersistentDataContainer, identificador de custom model data y atributos visuales, garantizando que solo ítems auténticos de Croppy puedan consumirse en mejoras.

---

## 4. Progresión Escalonada de Niveles (Tier Upgrades)

El sistema permite crear progresiones entre niveles. Por ejemplo, las regaderas superiores requieren la regadera del nivel inmediatamente inferior:

```
[Regadera de Madera] + Lingotes de Cobre ──> [Regadera de Cobre]
[Regadera de Cobre]  + Lingotes de Hierro ──> [Regadera de Hierro]
[Regadera de Hierro] + Lingotes de Oro   ──> [Regadera de Oro]
[Regadera de Oro]    + Diamantes         ──> [Regadera de Diamante]
[Regadera de Diamante] + Netherite       ──> [Regadera de Netherite]
```

Ejemplo de receta de mejora para `croppy:copper_can`:

```yaml
id: "croppy:copper_can"
type: "shaped"
group: "croppy_watering_cans"
category: "equipment"
result: "croppy:copper_can"
shape:
  - "C C"
  - "CWC"
  - " C "
ingredients:
  C: "minecraft:copper_ingot"
  W: "croppy:wooden_can"
```

---

## 5. Descubrimiento Automático en el Libro de Recetas

Por defecto, Croppy enseña automáticamente todas las recetas de crafteo registradas a los jugadores al unirse al servidor:

```yaml
# plugins/Croppy/config.yml
crafting:
  # Descubrir automáticamente todas las recetas en el libro del jugador al entrar al servidor
  auto-discover-recipes: true
```

Si se desactiva (`false`), los jugadores desbloquearán las recetas únicamente al obtener los ingredientes según la lógica vanilla o mediante comandos del servidor (`/recipe give <jugador> <receta>`).

---

## 6. Descubrimiento Declarativo en Addons

Tanto los addons Java compilados como los Content Packs declarativos pueden registrar recetas de crafteo:

* Ubica los archivos YAML en `<raiz-addon>/crafting/**/*.yml`.
* Croppy los analiza y registra en `CraftingRecipeRegistry` durante la habilitación del addon.
* Todas las claves registradas se rastrean en `AddonContext` y se eliminan limpiamente al deshabilitar o recargar el addon.

---

## 7. Recetas Iniciales Incluidas

Croppy incluye recetas de fábrica extraídas automáticamente en `plugins/Croppy/crafting/`:

* **Estaciones de Trabajo**:
  * `crafting/stations/cooking_pot.yml` (Caldero + 4 Lingotes de Hierro)
  * `crafting/stations/stove.yml` (Baldosas de Piedra Lisa + Ladrillos + Hoguera)
  * `crafting/stations/skillet.yml` (2 Lingotes de Hierro + Palo)
* **Herramientas Agrícolas**:
  * `crafting/tools/inspector.yml` (Panel de Cristal + Brújula + Palo)
  * `crafting/tools/thermometer.yml` (Panel de Cristal + Redstone + Palo)
* **Regaderas por Niveles**:
  * `crafting/watering_cans/wooden_can.yml` (Etiqueta de tablones + Cuenco)
  * `crafting/watering_cans/copper_can.yml` (Lingotes de Cobre + Regadera de Madera)
  * `crafting/watering_cans/iron_can.yml` (Lingotes de Hierro + Regadera de Cobre)
  * `crafting/watering_cans/golden_can.yml` (Lingotes de Oro + Regadera de Hierro)
  * `crafting/watering_cans/diamond_can.yml` (Diamantes + Regadera de Oro)
  * `crafting/watering_cans/netherite_can.yml` (Lingote de Netherite + Regadera de Diamante)
* **Fertilizantes**:
  * `crafting/fertilizers/growth_fertilizer.yml` (Polvo de Hueso + Carne Podrida)
  * `crafting/fertilizers/hydrating_fertilizer.yml` (Polvo de Hueso + Bola de Arcilla)
  * `crafting/fertilizers/quality_fertilizer.yml` (Polvo de Hueso + Polvo de Piedra Luminosa)
