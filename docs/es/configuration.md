---
title: Guía de Configuración
description: Referencia exhaustiva del archivo principal config.yml de Croppy, tasas de crecimiento, backends de almacenamiento (PDC vs SQLite) y ajustes de idioma.
sidebar:
  order: 3
---

# Guía de Configuración

El archivo de configuración principal de Croppy se encuentra en `plugins/Croppy/config.yml`. Permite ajustar los ciclos del motor de simulación, la selección del backend de persistencia y el idioma por defecto del servidor.

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

Croppy soporta dos arquitecturas de almacenamiento de alto rendimiento:

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

## 3. Localización e Idioma por Defecto (`locale`)

Croppy cuenta con un sistema integral de internacionalización multi-idioma. El idioma de reserva global para el servidor se configura en `config.yml`:

```yaml
locale:
  # Idioma por defecto cuando el cliente del jugador carece de traducción o para logs de consola.
  # Idiomas incluidos: en-US, es-ES (ver plugins/Croppy/locales/)
  default: "en-US"
```

* **`default`**: Código BCP-47 utilizado cuando un cliente carece de traducción o para los avisos en la consola del servidor. Por defecto: `en-US`.
* **Retrocompatibilidad**: Si una instalación existente tiene configurado `messages.default-locale`, el plugin lo detectará automáticamente como alternativa.
* **Personalización de Mensajes y Prefijos**: Todos los mensajes de chat, avisos de comandos, nombres de ítems y textos de interfaces se gestionan en los archivos YAML correspondientes dentro de `plugins/Croppy/locales/` (ej. `en-US.yml`, `es-ES.yml`). Para más detalles, consulta la [Guía de Localización](./localization.md).

---

## 4. Personalización de Herramientas y la Herramienta de Inspección

La herramienta de inspección (`croppy:inspection_tool`) se define de forma modular a través del **Registro de Ítems** en `plugins/Croppy/items/tools/inspector.yml`. Para personalizar su material, nombre visible, modelo 3D o lore, edita directamente su definición YAML o consulta la [Guía de Ítems y Herramientas](./items-and-tools.md).
