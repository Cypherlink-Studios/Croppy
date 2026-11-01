---
title: Configuration Guide
description: In-depth reference for Croppy's main config.yml, growth rates, storage backends (PDC vs SQLite), and inspection tool settings.
sidebar:
  order: 3
---

# Configuration Guide

The primary configuration for Croppy is located at `plugins/Croppy/config.yml`. This file controls core simulation loops, storage engine selection, tool appearances, and fallback messaging.

---

## 1. Simulation & Growth Settings (`growth`)

The `growth` block defines the master frequency and rules for the domain growth ticker:

```yaml
growth:
  # Frequency of simulation cycles in server ticks (20 ticks = 1 second)
  tick-interval: 20

  # Base progress percentage gained each cycle when all conditions are satisfied
  base-progress: 1.0

  # Horizontal search radius in blocks for detecting water sources (4 blocks = 9x9 area)
  water-search-radius: 4
```

### Explanations
* **`tick-interval`**: Lower values make growth checks more responsive to changing environmental conditions (light, rain), while higher values (e.g., `40` or `100`) reduce server CPU overhead. Default: `20` ticks (1 second).
* **`base-progress`**: The standard growth points awarded per cycle. A crop requiring 100 points will take 100 seconds of optimal conditions at `1.0` points/second.
* **`water-search-radius`**: Standard Minecraft water search radius for hydrating farmland. Set to `4` blocks to match vanilla farmland hydration logic.

---

## 2. Storage Engines (`storage`)

Croppy supports two high-performance storage architectures:

```yaml
storage:
  # Storage engine backend type: "pdc" (recommended) or "sqlite"
  type: "pdc"

  sqlite:
    # SQLite database filename inside the plugin directory
    file: "crops.db"
    pool:
      maximum-pool-size: 5
      connection-timeout: 30000
```

### Comparing Storage Engines

| Feature | PDC (`pdc`) | SQLite (`sqlite`) |
|---|---|---|
| **Underlying Mechanism** | Paper Chunk `PersistentDataContainer` | Embedded SQLite via HikariCP connection pool |
| **I/O Overhead** | Zero disk I/O outside normal chunk saves | Asynchronous batch writes to disk |
| **External Dependencies** | None (pure native Paper API) | Dynamically loaded SQLite JDBC driver |
| **World Backups** | Seamlessly bundled within world region files | Separate `.db` file in `plugins/Croppy/` |
| **Best Used For** | Most servers; optimal chunk lifecycle alignment | Cross-chunk queries, analytics, or external tools |

> **Recommendation**: Leave `type: "pdc"` for zero-configuration, native world-synced chunk persistence. If you select `sqlite`, the SQLite JDBC driver and HikariCP pool are downloaded automatically at first boot via Libby.

---

## 3. Inspection Tool (`inspection-tool`)

Customizes the item used to inspect crop states:

```yaml
inspection-tool:
  material: "SPYGLASS"
  name: "<gradient:#70e000:#38b000><bold>Crop Inspector</bold></gradient>"
  lore:
    - "<gray>Right-click any crop to view"
    - "<gray>its current growth status."
```

* **`material`**: Any valid Bukkit `Material` enum name (e.g. `SPYGLASS`, `STICK`, `COMPASS`).
* **`name` & `lore`**: Formatted with Kyori Adventure MiniMessage tags.

---

## 4. Default Messages & Locale Fallback (`messages`)

```yaml
messages:
  default-locale: "en-US"
  prefix: "<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>"
  no-permission: "<red>You do not have permission to execute this command."
  reload-success: "<green>Configuration and species definitions reloaded successfully."
```

* **`default-locale`**: The BCP-47 locale tag used when a player's client language is not translated or when messages are dispatched to the server console. Default: `en-US`.
* For in-depth message overrides and translations, see the [Localization Guide](./localization.md).
