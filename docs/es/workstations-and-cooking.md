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

## 3. Flujo Interactivo de Cocción y Ejecución de Procesos

Croppy v0.5.0 incorpora flujos de cocina dinámicos adaptados a los modelos de interacción de cada estación:

### Flujo en Sartén (Interacción Física Directa)
1. **Calentar la Sartén**: Coloca la sartén sobre una fuente de calor activa (fogata encendida o estufa). Deja que la temperatura alcance el umbral mínimo de la receta (ej. 120°C para huevos fritos).
2. **Colocar Ingredientes**: Haz clic derecho en la sartén con el ingrediente en mano (ej. un huevo). El ítem se acoplará sobre la superficie chisporroteante mediante displays virtuales 3D de PacketEvents.
3. **Disparo Automático del Proceso**: En cuanto se cumplan los ingredientes requeridos y las condiciones térmicas de inicio, el proceso de cocción secuencial comenzará automáticamente.
4. **Recoger el Plato**: Al llegar al 100% de progreso, haz clic derecho con la mano vacía para recoger el plato recién cocinado.

### Flujo en Olla de Cocina (Híbrida / Menú GUI)
1. **Abrir el Menú de la Olla**: Haz clic derecho sobre la olla situada sobre una fuente de calor para desplegar su menú matricial interactivo 9xN.
2. **Introducir Ingredientes**: Coloca las verduras, carnes, caldos o cuencos en los slots de entrada.
3. **Encender Utensilio (Botón 'B')**: Haz clic en el botón de acción **Encender Utensilio (`START_PROCESS`)**:
   * El motor valida la coincidencia con una receta válida y las condiciones térmicas de inicio.
   * Los ingredientes de entrada se consumen inmediatamente de la matriz.
   * Los slots de entrada se bloquean y actualizan, y el proceso inicia con efectos de audio y partículas de salpicadura.
4. **Poblado Automático de Resultados (`populateOutputContents`)**: Al finalizar el tiempo, los platos terminados se depositan automáticamente en los slots de salida. Si dichos slots estuvieran llenos, los platos caerán de forma segura al suelo junto a la olla para evitar pérdidas.

### Pausa Térmica y Seguridad en Cocción
Si la fuente de calor bajo la estación se apaga durante la cocción, el proceso activo entra automáticamente en **pausa** (`pause_on_condition_failure`). El progreso y los segundos transcurridos quedan preservados hasta que la fuente de calor se vuelva a encender.

---

## 4. Reglas de Cocción y Quemado

Cocinar en Croppy exige atención por parte del jugador:

* **Zonas Térmicas Requeridas**: Cada receta exige una temperatura mínima (ej. Fuego Lento, Ebullición). Una olla fría no avanzará el progreso de la receta.
* **Sobrecalentamiento y Quemado (`rules/overcooking_burn.yml`)**: Dejar una comida lista al fuego tras alcanzar el 100% de cocción activará la regla de sobrecocción, convirtiendo el plato en cenizas o carbón si no se retira a tiempo.
* **Quemado Térmico (`rules/thermal_burn.yml`)**: Someter ingredientes delicados a temperaturas extremas que superen su tolerancia máxima arruinará de inmediato la elaboración.

---

## 5. Orientación Dinámica y Apilamiento de Ítems

* **Frente Orientado al Jugador**: Las estaciones colocadas en el mundo se orientan dinámicamente hacia el jugador usando ajuste cardinal (`CARDINAL_4`, `CARDINAL_8`) o ángulo continuo (`FREE_YAW`). Los modelos virtuales 3D y los displays de ingredientes hijos rotan solidariamente con esta orientación.
* **Apilamiento Nativo de Ítems**: Los ingredientes extraídos de las estaciones conservan los metadatos vanilla limpios. Los cultivos, carnes o condimentos extraídos de ollas o sartenes se apilan sin problemas con ítems idénticos del inventario del jugador.
