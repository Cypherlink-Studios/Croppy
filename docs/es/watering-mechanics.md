---
title: Mecánicas de Riego
description: Guía completa de regaderas por niveles, área de efecto de hidratación, recarga en fuentes de agua y retención de humedad.
sidebar:
  order: 8
---

# Mecánicas de Riego e Hidratación

El agua es un recurso indispensable para el crecimiento agrícola en Croppy. Mantener la tierra hidratada garantiza que los cultivos avancen de fase sin pausas. Para acompañar la progresión del jugador, Croppy incorpora un sistema de **Regaderas** organizadas por niveles de capacidad y área de efecto (AoE).

---

## 1. Niveles de Regaderas y Especificaciones

Las regaderas almacenan cargas de agua en su PersistentDataContainer (PDC). Al mejorar los materiales, aumentan tanto la autonomía como la superficie de riego:

| Ítem | Nombre Visible | Material Base | Capacidad | Área de Riego (AoE) |
|---|---|---|---|---|
| `croppy:wooden_can` | **Regadera de Madera** | `WOODEN_HOE` | **20 usos** | **1x1** (1 bloque) |
| `croppy:copper_can` | **Regadera de Cobre** | `COPPER_INGOT` | **50 usos** | **1x3** (Línea de 3 bloques) |
| `croppy:iron_can` | **Regadera de Hierro** | `IRON_INGOT` | **100 usos** | **1x3** (Línea de 3 bloques) |
| `croppy:golden_can` | **Regadera de Oro** | `GOLD_INGOT` | **250 usos** | **3x3** (Cuadrado de 9 bloques) |
| `croppy:diamond_can` | **Regadera de Diamante** | `DIAMOND` | **500 usos** | **3x3** (Cuadrado de 9 bloques) |
| `croppy:netherite_can` | **Regadera de Netherite** | `NETHERITE_INGOT` | **1000 usos** | **5x5** (Cuadrado de 25 bloques) |

---

## 2. Cómo Recargar las Regaderas

Cada acción de riego consume una carga. Cuando la regadera se vacía, la barra de acción del jugador emite un aviso (`watering.empty`).

Para recargar la regadera al máximo:
1. Localiza cualquier fuente de agua válida:
   * Bloques de fuente de agua natural (`minecraft:water`), océanos, ríos o lagos.
   * **Bloques anegados (*waterlogged*)** (losas, escaleras, vallas, hojas).
   * **Calderos con agua** (`WATER_CAULDRON` con nivel de agua > 0).
2. Sostén la regadera en la mano principal y haz **Clic Derecho** en el agua.
3. **Trazado de Rayos de Fluidos (*Raytracing*)**: También puedes hacer clic derecho al aire apuntando hacia una superficie con agua en un rango de hasta 5 bloques. El motor detectará el fluido en el servidor y rellenará la regadera.
4. Un sonido de agua y partículas de burbujas confirmarán que la regadera se ha recargado por completo.

---

## 3. Dinámica de Humedad en la Tierra Arada

* **Nivel Máximo (8/8)**: Al regar con la regadera, el nivel de hidratación del bloque sube instantáneamente a 8.
* **Evaporación**: Con el paso de los ciclos o el inicio de un nuevo día, la tierra pierde humedad gradualmente a menos que cuente con canales de agua adyacentes o `croppy:hydrating_fertilizer`.
* **Comprobación Diaria**: Los cultivos requieren haber recibido agua en el día del mundo actual para mantener su ritmo de crecimiento sin interrupciones.
