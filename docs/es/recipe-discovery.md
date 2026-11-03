---
title: Descubrimiento de Recetas
description: Guía del motor de progresión culinaria, estados de conocimiento (Bloqueado, Con Pista, Desbloqueado) y recetas secretas.
sidebar:
  order: 12
---

# Motor de Descubrimiento de Recetas

Croppy premia la curiosidad y la experimentación culinaria a través de su **Motor de Descubrimiento de Recetas**. En lugar de mostrar todas las recetas desde el primer minuto, los secretos gastronómicos se revelan a medida que el jugador cosecha ingredientes raros, interactúa con estaciones o prueba combinaciones nuevas.

---

## 1. Estados de Conocimiento del Jugador

Para cada receta registrada en el servidor, cada jugador cuenta con un estado de conocimiento individualizado:

```text
+-----------------------------------------------------------------------------+
|                      PROGRESIÓN DE CONOCIMIENTO DE RECETAS                  |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [1. BLOQUEADO] (LOCKED)                                                    |
|  La receta es un misterio completo. Se muestra como una silueta oculta o    |
|  un signo de interrogación en el Compendio Culinario.                       |
|                                     |                                       |
|                                     v (El jugador cumple criterios de pista)|
|  [2. CON PISTA] (HINTED)                                                    |
|  Aparece el título de la receta junto con un acertijo o lista parcial de    |
|  ingredientes para guiar al cocinero en sus experimentos.                   |
|                                     |                                       |
|                                     v (Cocción exitosa o evento de desbloqueo)|
|  [3. DESBLOQUEADO] (UNLOCKED)                                               |
|  ¡Receta dominada! La matriz de ingredientes completa, zonas térmicas       |
|  óptimas y cantidades resultantes quedan registradas permanentemente.       |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Criterios y Disparadores de Descubrimiento

Las recetas pueden configurar disparadores automáticos:

* **Cosecha Agrícola**: Al recolectar una variedad vegetal por primera vez, se revelan recetas donde interviene ese fruto.
* **Obtención de Insumos**: Al conseguir especias o harinas especiales, los platos asociados pasan de `LOCKED` a `HINTED`.
* **Experimentación en Estación**: Si un jugador reúne en la olla o sartén los ingredientes exactos de una receta secreta y aplica la temperatura correcta, la receta **se desbloqueará automáticamente al finalizar la cocción** (`DiscoveryResult.UNLOCKED_EXPERIMENT`).
* **Concesión Administrativa**: Los administradores o sistemas de misiones pueden otorgar recetas directamente mediante `/croppy recipe give <jugador> <recipe_id>`.

---

## 3. Recetas Secretas

Las preparaciones marcadas con `secret: true` en su configuración no aparecen en el recetario ni como silueta ni como espacio vacío. Se mantienen en secreto absoluto hasta que el jugador las descubre cocinando por intuición, momento en el que se reproduce un sonido de logro y una fanfarria visual.
