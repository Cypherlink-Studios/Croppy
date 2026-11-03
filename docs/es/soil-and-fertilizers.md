---
title: Suelo y Fertilizantes
description: Guía de química de nutrientes del suelo (N-P-K), fertilizantes especiales, métodos de aplicación y retención de humedad.
sidebar:
  order: 7
---

# Química del Suelo y Fertilizantes

En Croppy, la tierra arada funciona como un depósito bioquímico activo. El suelo acumula nutrientes y puede enriquecerse mediante fertilizantes para acelerar el crecimiento, incrementar la calidad de la cosecha o retener la hidratación matutina.

---

## 1. Nutrientes del Suelo (Modelo N-P-K)

Cada bloque de tierra arada gestiona tres macronutrientes esenciales:

* **Nitrógeno (N)**: Fomenta el crecimiento vegetativo, el follaje y la rapidez de los primeros brotes.
* **Fósforo (P)**: Fortalece las raíces y agiliza la transición entre las etapas intermedias y florales.
* **Potasio (K)**: Incrementa la resistencia general de la planta y la probabilidad de obtener cosechas de mayor calidad.

A medida que las plantas crecen, consumen parte de estos nutrientes. Los agricultores pueden revisar el balance con el **Inspector de Cultivos** o aplicar fertilizantes para recuperar la fertilidad del terreno.

---

## 2. Tipos de Fertilizantes

Croppy incluye tres tipos principales de fertilizantes configurados en `items/fertilizers/`:

| Clave del Ítem | Material Base | Clasificación | Efecto Principal |
|---|---|---|---|
| `croppy:speed_gro` | `BONE_MEAL` | **`SPEED`** | Acelera la velocidad de crecimiento en un **+25%** (`speed_multiplier: 1.25`). |
| `croppy:quality_fertilizer` | `GLOWSTONE_DUST` | **`QUALITY`** | Otorga un bono del **+20%** (`quality_boost: 0.20`) para cosechar productos de calidad superior. |
| `croppy:retaining_soil` | `CLAY_BALL` | **`RETAINING`** | Ofrece un **33% de probabilidad** (`retention_chance: 0.33`) de conservar la humedad al amanecer sin regar. |

---

## 3. Cómo Aplicar Fertilizantes

1. **Preparar el Terreno**: Ara la tierra con una azada.
2. **Aplicación**: Sostén el fertilizante en la mano principal y haz **Clic Derecho** sobre la tierra arada (antes o después de plantar las semillas).
3. **Efectos Visuales**: Partículas verdes y doradas confirman que las propiedades del fertilizante se han registrado en el PersistentDataContainer (PDC) del bloque.
4. **Regla de No Acumulación**: Cada bloque de cultivo admite un único fertilizante activo a la vez para mantener el equilibrio del juego.

---

## 4. Especificación del Esquema YAML de Fertilizantes

Los fertilizantes se declaran en `plugins/Croppy/items/fertilizers/*.yml` y son analizados por `YamlFertilizerLoader`.

### Ejemplo Completo YAML (`speed_gro.yml`)

```yaml
id: "croppy:speed_gro"
category: "fertilizers"
name: "<green>Fertilizante Speed-Gro</green>"
lore:
  - "<gray>Acelera la velocidad de crecimiento del cultivo en un 25%.</gray>"

# Clasificación funcional: SPEED, QUALITY o RETAINING
type: SPEED

# Multiplicadores numéricos según clasificación:
speed_multiplier: 1.25
# quality_boost: 0.20     # (Para tipo QUALITY)
# retention_chance: 0.33   # (Para tipo RETAINING)

# Representación del ítem físico
item:
  material: BONE_MEAL
  custom_model_data: 1001
  item-model: "croppy:speed_gro"

# Etiquetas PDC vinculadas
pdc:
  type: "SPEED"
  speed_multiplier: "1.25"
```

### Tabla de Referencia de Campos

| Ruta del Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `id` | String (`Key`) | **Sí** | Clave única con namespace (ej. `croppy:speed_gro`). |
| `type` | Enum | **Sí** | Clasificación: `SPEED`, `QUALITY` o `RETAINING`. |
| `speed_multiplier` | Double | Para `SPEED` | Multiplicador de crecimiento (ej. `1.25` para +25% de velocidad). |
| `quality_boost` | Double | Para `QUALITY` | Modificador de tirada de calidad (ej. `0.20` para +20% de probabilidad). |
| `retention_chance` | Double | Para `RETAINING` | Probabilidad (0.0 a 1.0) de que la tierra retenga el agua al amanecer. |
| `item.material` | String | **Sí** | Material base Bukkit/Minecraft. |
| `item.item-model` | String | No | Identificador del Data Component `item_model` para 1.21.4+. |
| `item.custom_model_data` | Entero | No | Valor numérico CustomModelData para compatibilidad clásica. |
