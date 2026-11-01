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
| `croppy:rusty_can` | **Regadera Oxidada** | `COPPER_INGOT` | **20 usos** | **1x1** (1 bloque) |
| `croppy:copper_can` | **Regadera de Cobre** | `COPPER_INGOT` | **40 usos** | **1x3** (Línea de 3 bloques) |
| `croppy:silver_can` | **Regadera de Plata** | `IRON_INGOT` | **100 usos** | **3x3** (Cuadrado de 9 bloques) |
| `croppy:gold_can` | **Regadera de Oro** | `GOLD_INGOT` | **250 usos** | **3x3** (Cuadrado de 9 bloques) |
| `croppy:iridium_can` | **Regadera de Iridio** | `NETHERITE_INGOT` | **600 usos** | **5x5** (Cuadrado de 25 bloques) |

---

## 2. Cómo Recargar las Regaderas

Cada acción de riego consume una carga. Cuando la regadera se vacía, la barra de acción del jugador emite un aviso (`watering.empty`).

Para recargar la regadera al máximo:
1. Acércate a cualquier bloque de fuente de agua natural (`minecraft:water`), río, océano o caldero con agua.
2. Sostén la regadera en la mano principal y haz **Clic Derecho** en el agua.
3. Un sonido de agua y partículas de burbujas confirmarán que la regadera se ha recargado por completo.

---

## 3. Dinámica de Humedad en la Tierra Arada

* **Nivel Máximo (8/8)**: Al regar con la regadera, el nivel de hidratación del bloque sube instantáneamente a 8.
* **Evaporación**: Con el paso de los ciclos o el inicio de un nuevo día, la tierra pierde humedad gradualmente a menos que cuente con canales de agua adyacentes o `croppy:retaining_soil`.
* **Comprobación Diaria**: Los cultivos requieren haber recibido agua en el día del mundo actual para mantener su ritmo de crecimiento sin interrupciones.
