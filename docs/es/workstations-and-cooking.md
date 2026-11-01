---
title: Estaciones y Cocina
description: Guía de estaciones de cocina físicas (Olla de Cocina, Sartén), displays 3D, cocción térmica y mecánicas de quemado.
sidebar:
  order: 9
---

# Estaciones de Cocina y Mecánicas Culinarias

Croppy convierte la cocina en un proceso visual y táctil. Los chefs interactúan con estaciones físicas colocadas en el mundo, observan los ingredientes mediante displays virtuales impulsados por PacketEvents y deben vigilar la temperatura para no arruinar sus preparaciones.

---

## 1. Estaciones de Cocina Disponibles

Croppy incluye dos estaciones de trabajo culinarias principales:

```text
+-----------------------------------------------------------------------------+
|                            ESTACIONES DE COCINA                             |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [OLLA DE COCINA] (croppy:cooking_pot)                                      |
|  - Bloque: Cabeza de jugador (texturas de caldero y ollas)                  |
|  - Interacción: HÍBRIDA (Clic físico en bloque o menú GUI 9xN)              |
|  - Capacidad: 6 ingredientes de entrada, 2 platos de salida                 |
|  - Ideal Para: Sopas, guisos, salsas, mermeladas y caldos a fuego lento     |
|                                                                             |
|  [SARTÉN] (croppy:skillet)                                                  |
|  - Bloque: Placa de presión pesada                                          |
|  - Interacción: FÍSICA (Inserción y extracción con clic derecho directo)    |
|  - Capacidad: 4 ingredientes de entrada, 2 platos de salida                 |
|  - Ideal Para: Carnes selladas, salteados, panqueques y frituras            |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Displays Virtuales con PacketEvents

En lugar de utilizar Armor Stands reales (que saturan el servidor y dejan entidades huérfanas al reiniciar), Croppy emplea **PacketEvents** para enviar paquetes virtuales al cliente:

* **Previsualización de Ingredientes**: Al colocar insumos en la sartén o en la olla, un Item Display virtual flota sobre la superficie de cocción con escalas y rotaciones ajustadas.
* **Hologramas de Estado en Tiempo Real**: Un Text Display en modo cartel flota sobre la estación mostrando:
  * Nombre de la Receta (`{recipe_name}`)
  * Barra de Progreso animada (`{progress_bar}`) y porcentaje (`{progress_percent}%`)
  * Temperatura actual (`{temperature_c}`) y zona térmica activa (`{thermal_zone}`)
* **Culling por Distancia**: Los displays virtuales desaparecen automáticamente fuera del rango visual configurado (`8-24 bloques`), manteniendo altos los FPS del cliente.

---

## 3. Reglas de Cocción y Quemado

Cocinar en Croppy exige atención por parte del jugador:

* **Zonas Térmicas Requeridas**: Cada receta exige una temperatura mínima (ej. Fuego Lento, Ebullición). Una olla fría no avanzará el progreso de la receta.
* **Sobrecalentamiento y Quemado (`rules/overcooking_burn.yml`)**: Dejar una comida lista al fuego tras alcanzar el 100% de cocción activará la regla de sobrecocción, convirtiendo el plato en cenizas o carbón si no se retira a tiempo.
* **Quemado Térmico (`rules/thermal_burn.yml`)**: Someter ingredientes delicados a temperaturas extremas que superen su tolerancia máxima arruinará de inmediato la elaboración.
