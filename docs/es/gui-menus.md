---
title: Menús Configurables
description: Guía del sistema de inventarios matriciales 9xN, asignación de slots de entrada y salida, estados condicionales y acciones.
sidebar:
  order: 13
---

# Sistema de Menús Configurables

Croppy cuenta con un motor declarativo de menús en `plugins/Croppy/menus/`. Los administradores pueden rediseñar por completo la disposición de las estaciones de cocina, el fogón de la estufa y el compendio utilizando matrices visuales de caracteres ASCII.

---

## 1. Diseño de la Matriz y Patrones de Casillas

Cada menú define sus dimensiones (`rows: 1..6`) y un esquema de caracteres donde cada símbolo corresponde a un elemento funcional:

```yaml
id: "croppy:cooking_pot"
title: "<gradient:#ffaa00:#ff5555>Olla de Cocina</gradient>"
rows: 4

pattern:
  - "X X X X X X X X X"
  - "X I I I . O O . X"
  - "X I I I . S . . X"
  - "X X X X B X X X X"
```

### Significado de los Símbolos
* **`X`**: Paneles de cristal o decoración estética de borde.
* **`I`**: Casillas de entrada (INPUT) donde se colocan los ingredientes crudos.
* **`O`**: Casillas de salida (OUTPUT) donde aparecen los platos terminados.
* **`S`**: Indicador dinámico de estado (STATUS) que refleja el avance de la cocción.
* **`B`**: Botones interactivos de acción (ej. encender, iniciar proceso, cancelar).
* **`.`**: Espacios vacíos interactuables.

---

## 2. Tipos de Elementos

### A. Casillas de Entrada y Salida (`INPUT` / `OUTPUT`)
Gestionan la colocación de ingredientes y la recogida de productos:
```yaml
elements:
  'I':
    type: INPUT
  'O':
    type: OUTPUT
```

### B. Elementos de Estado Dinámicos (`STATUS`)
Cambian su icono, nombre y lore en tiempo real evaluando condiciones booleanas:
```yaml
elements:
  'S':
    type: STATUS
    item:
      material: "minecraft:cauldron"
      name: "<gray>Olla: <italic>En espera</italic></gray>"
    states:
      - condition: "process.state == 'RUNNING'"
        item:
          material: "minecraft:campfire"
          name: "<gold><bold>Cocinando...</bold></gold>"
          lore:
            - "<yellow>Receta:</yellow> <white>{recipe.name}</white>"
            - "<yellow>Progreso:</yellow> <aqua>{process.progress_percent}%</aqua>"
            - "<yellow>Calor:</yellow> <white>{station.heat_level}°C</white>"
          glow: true

      - condition: "process.state == 'STALLED'"
        item:
          material: "minecraft:barrier"
          name: "<red><bold>¡Detenido: Falta calor!</bold></red>"
```

### C. Botones de Acción (`ACTION`)
Ejecutan operaciones del plugin al recibir clics específicos (`LEFT`, `RIGHT`, `SHIFT_LEFT`):
```yaml
elements:
  'B':
    type: ACTION
    item:
      material: "minecraft:flint_and_steel"
      name: "<yellow><bold>Encender Fuego</bold></yellow>"
    actions:
      - click: LEFT
        type: STATION_ACTION
        action: "START_PROCESS"
```
