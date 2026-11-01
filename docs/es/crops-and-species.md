---
title: Cultivos y Especies
description: Guía de configuración de especies agrícolas en YAML, etapas de crecimiento, requisitos ambientales y tablas de cosecha.
sidebar:
  order: 5
---

# Configuración de Cultivos y Especies

En Croppy, cada variedad agrícola se define como una **Especie** lógica dentro de `plugins/Croppy/crops/<nombre>.yml`. El plugin desacopla la lógica interna del estado físico del bloque de Minecraft, permitiendo curvas de crecimiento personalizadas, fases de vida configurables y tablas de botín a medida.

---

## 1. Esquema YAML de una Especie

A continuación se muestra la configuración base de `wheat.yml`:

```yaml
# Identificador semántico único en formato <namespace>:<valor>
id: "croppy:wheat"

# Nombre visible por defecto (puede traducirse mediante i18n)
name: "Trigo"

display:
  # Modelo visual: "vanilla" (edad del bloque) o "custom" (displays)
  type: "vanilla"
  # Bloque vanilla colocado en el mundo
  block: "minecraft:wheat"
  # Ítem custom necesario para plantar esta semilla
  seed_item: "croppy:wheat_seeds"

stages:
  - id: "seed"
    progress_required: 100
    display:
      block_age: 0

  - id: "sprout"
    progress_required: 100
    display:
      block_age: 2

  - id: "growing"
    progress_required: 150
    display:
      block_age: 5

  - id: "mature"
    progress_required: 0
    display:
      block_age: 7

requirements:
  # Nivel de luz mínimo en el bloque (0-15) requerido para la fotosíntesis
  min_light: 9
  # Nivel de fluido o agua requerido en el entorno cercano
  water_fluid_level: 1

harvest:
  # Ítems obtenidos al romper o cosechar la planta madura
  yield:
    - item: "minecraft:wheat"
      min: 1
      max: 1
    - item: "croppy:wheat_seeds"
      min: 1
      max: 3
```

---

## 2. Modos de Representación Visual

Croppy admite dos estrategias para mostrar cultivos en el mundo:

### A. Basados en Vanilla (`type: "vanilla"`)
Vincula las etapas de crecimiento a las edades de los bloques agrícolas de Minecraft (`block_age: 0..7`).
* No requiere resource pack en el cliente.
* Compatible con `minecraft:wheat`, `minecraft:carrots`, `minecraft:potatoes`, `minecraft:beetroots` y `minecraft:pitcher_crop`.
* La apariencia cambia de forma fluida a medida que el cultivo avanza de etapa.

### B. Displays Personalizados (`type: "custom"`)
Permite representar cultivos no convencionales (como árboles frutales en miniatura o plantas místicas) usando bloques base invisibles y entidades virtuales de visualización.

---

## 3. Etapas y Puntos de Progreso

Cada etapa define un `id` y los puntos necesarios (`progress_required`):
* Al acumular suficientes puntos de crecimiento, la planta pasa a la fase siguiente.
* La fase final (`mature`) define `progress_required: 0`, indicando que el cultivo está completamente listo para cosechar.

---

## 4. Tablas de Cosecha (`harvest.yield`)

El bloque `yield` permite configurar el botín al cosechar:
* **`item`**: Puede ser un ítem vanilla (`minecraft:wheat`) o un ítem de Croppy (`croppy:wheat_seeds`).
* **`min` y `max`**: Rango de cantidades distribuidas uniformemente.
