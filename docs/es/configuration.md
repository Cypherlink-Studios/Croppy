---
title: Guía de Configuración
description: Referencia exhaustiva del archivo principal config.yml de Croppy, tasas de crecimiento, backends de almacenamiento (PDC vs SQLite) y ajustes de herramientas.
sidebar:
  order: 3
---

# Guía de Configuración

El archivo de configuración principal de Croppy se encuentra en `plugins/Croppy/config.yml`. Permite ajustar los ciclos de simulación, la persistencia en disco, las herramientas de diagnóstico y los textos por defecto.

---

## 1. Simulación y Crecimiento (`growth`)

El bloque `growth` controla el ciclo de vida del motor de simulación agrícola:

```yaml
growth:
  # Frecuencia de los ciclos en ticks del servidor (20 ticks = 1 segundo)
  tick-interval: 20

  # Porcentaje base de progreso acumulado por ciclo cuando se cumplen los requisitos
  base-progress: 1.0

  # Radio horizontal de búsqueda en bloques para detectar agua (4 bloques = área 9x9)
  water-search-radius: 4
```

### Explicación de Parámetros
* **`tick-interval`**: Un valor bajo hace que el cultivo reaccione más rápido a los cambios de luz y lluvia; valores más altos (ej. `40` o `100`) minimizan el consumo de CPU en servidores muy concurridos. Por defecto: `20` ticks (1 segundo).
* **`base-progress`**: Puntos estándar de progreso otorgados por segundo óptimo. Un cultivo que requiera 100 puntos tardará 100 segundos bajo condiciones ideales a `1.0` puntos/segundo.
* **`water-search-radius`**: Distancia de búsqueda de fuentes de agua para hidratar tierra arada, equivalente al comportamiento vanilla (4 bloques).

---

## 2. Motores de Almacenamiento (`storage`)

Croppy soporta dos arquitecturas de almacenamiento:

```yaml
storage:
  # Tipo de backend: "pdc" (recomendado) o "sqlite"
  type: "pdc"

  sqlite:
    # Nombre del archivo de base de datos dentro de plugins/Croppy/
    file: "crops.db"
    pool:
      maximum-pool-size: 5
      connection-timeout: 30000
```

### Comparativa de Backends

| Característica | PDC (`pdc`) | SQLite (`sqlite`) |
|---|---|---|
| **Mecanismo Subyacente** | `PersistentDataContainer` del Chunk en Paper | SQLite embebido con pool de conexiones HikariCP |
| **Sobrecarga de I/O** | Cero escrituras adicionales fuera del guardado de chunks | Guardado asíncrono en segundo plano |
| **Dependencias Externas** | Ninguna (API nativa de Paper) | Driver JDBC descargado en el primer inicio vía Libby |
| **Copias de Seguridad** | Integrado de forma natural en los archivos de región del mundo | Archivo `.db` independiente en `plugins/Croppy/` |
| **Uso Recomendado** | Servidores de supervivencia estándar; perfecta sincronización | Servidores con analítica externa o consultas entre chunks |

> **Recomendación**: Mantén `type: "pdc"` para una persistencia ligera y nativa sin configuraciones adicionales.

---

## 3. Herramienta de Inspección (`inspection-tool`)

Personaliza el ítem utilizado para inspeccionar cultivos:

```yaml
inspection-tool:
  material: "SPYGLASS"
  name: "<gradient:#70e000:#38b000><bold>Inspector de Cultivos</bold></gradient>"
  lore:
    - "<gray>Haz clic derecho sobre cualquier cultivo"
    - "<gray>para consultar su estado de crecimiento."
```

* **`material`**: Cualquier nombre válido del enum `Material` de Bukkit (ej. `SPYGLASS`, `STICK`, `COMPASS`).
* **`name` & `lore`**: Formateados con etiquetas de Kyori Adventure MiniMessage.

---

## 4. Mensajes y Fallback de Idioma (`messages`)

```yaml
messages:
  default-locale: "en-US"
  prefix: "<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>"
  no-permission: "<red>No tienes permisos para ejecutar este comando."
  reload-success: "<green>Configuración y definiciones recargadas con éxito."
```

* **`default-locale`**: Código BCP-47 utilizado cuando un cliente carece de traducción o para los avisos en la consola del servidor. Por defecto: `en-US`.
* Para personalizar todas las traducciones, consulta la [Guía de Localización](./localization.md).
