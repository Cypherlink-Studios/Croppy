---
title: Crecimiento y Entorno
description: Análisis exhaustivo del motor de simulación de crecimiento, factores ambientales, hidratación diaria e invernaderos.
sidebar:
  order: 6
---

# Simulación de Crecimiento y Factores Ambientales

Croppy sustituye el comportamiento probabilístico de los ticks vanilla por un **Motor de Crecimiento** determinista. Cada cultivo evalúa de forma periódica las condiciones físicas de su entorno y acumula puntos de progreso hacia su madurez.

---

## 1. El Ciclo de Simulación Agrícola

En cada ciclo (configurado en `growth.tick-interval`, por defecto 20 ticks = 1 segundo), se ejecuta la siguiente cadena de evaluación:

```text
+-----------------------------------------------------------------------------+
|                     CANAL DE EVALUACIÓN DE CRECIMIENTO                      |
+-----------------------------------------------------------------------------+
|                                                                             |
|  1. Verificación de Chunk --> Comprueba que el chunk del cultivo esté       |
|                               cargado en memoria antes de evaluar bloques.  |
|                                                                             |
|  2. Comprobación de Madurez -> Si ya está en etapa 'mature', se omite.      |
|                                                                             |
|  3. Requisitos Ambientales --> Comprueba luz mínima y nivel de agua.        |
|                               Si no se cumplen, el crecimiento se detiene.  |
|                                                                             |
|  4. Estado de Hidratación ---> Si el cultivo no recibió agua en el día,     |
|                               aplica la política de marchitamiento o pausa. |
|                                                                             |
|  5. Modificadores Activos ---> Multiplica baseProgress por los bonos de     |
|                               fertilizantes aplicados a la tierra.          |
|                                                                             |
|  6. Avance de Etapa --------> Suma el progreso. Si supera lo requerido,     |
|                               pasa a la siguiente etapa y actualiza display.|
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Requisitos Ambientales

### Nivel de Luz (`min_light`)
Las plantas requieren luz para desarrollarse:
* Evalúa la iluminación total del bloque (luz solar directa o fuentes artificiales como antorchas y faroles).
* Si una especie requiere `min_light: 9` y la zona tiene nivel `7`, el cultivo pausará su progreso hasta recibir iluminación.
* Los invernaderos o cultivos subterráneos pueden prosperar las 24 horas si cuentan con una iluminación artificial adecuada.

### Presencia de Agua (`water_fluid_level`)
* **Búsqueda en Radio Horizontal**: Croppy escanea los bloques cercanos (por defecto en un radio de 4 bloques, un área de 9x9) buscando fuentes de agua o bloques con agua.
* Una fuente de agua aporta un nivel de fluido `8`, mientras que las corrientes aportan entre `1` y `7`.

---

## 3. Riego Diario y Marchitamiento

Cuando se activa la comprobación de riego diario, cada cultivo comprueba si ha recibido agua durante el día del mundo actual. Si no se riega:

* **Pausa (Por defecto)**: El cultivo simplemente no acumula progreso durante ese ciclo, sin sufrir penalizaciones de vida.
* **Marchitamiento**: Tras varios días consecutivos sin hidratación, la planta puede degradar su salud o retroceder etapas hasta convertirse en un arbusto seco.

---

## 4. Modificadores y Fertilizantes

El motor permite aplicar potenciadores de velocidad a través de la tubería de modificadores:
* **Fertilizantes de Crecimiento Rápido**: Aplican multiplicadores (ej. `1.25x` de velocidad) a los puntos otorgados por ciclo.
* **Nutrientes del Suelo**: Niveles óptimos de N-P-K reducen el tiempo total necesario para alcanzar la cosecha.
