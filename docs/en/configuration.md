---
title: Configuration Guide
description: In-depth reference for Croppy's main config.yml, growth rates, storage backends (PDC vs SQLite), and default locale settings.
sidebar:
  order: 3
---

# Configuration Guide

The primary configuration for Croppy is located at `plugins/Croppy/config.yml`. This file controls core simulation loops, storage engine selection, and server default locale resolution.

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

## 3. Localization & Default Locale (`locale`)

Croppy features a full multi-locale internationalization system. The server-wide fallback locale is configured in `config.yml`:

```yaml
locale:
  # Default server locale when a player's client language is unavailable or for console logs.
  # Bundled locales: en-US, es-ES (see plugins/Croppy/locales/)
  default: "en-US"
```

* **`default`**: The BCP-47 locale tag used when a player's client language is not translated or when messages are dispatched to the server console. Default: `en-US`.
* **Backward Compatibility**: If an existing installation defines `messages.default-locale`, the plugin automatically reads it as a fallback.
* **Customizing Messages & Prefixes**: All chat messages, command notifications, item names, and UI labels are managed through per-locale YAML bundles in `plugins/Croppy/locales/` (e.g. `en-US.yml`, `es-ES.yml`). For complete details, see the [Localization Guide](./localization.md).

---

## 4. Third-Party Integrations (`integrations`)

Croppy supports seamless, modular integrations with WorldGuard, PlaceholderAPI, and Vault:

```yaml
integrations:
  worldguard:
    # Enables land claim and region protection checks via WorldGuard
    enabled: true
    # Registers custom WorldGuard flags (croppy-plant, croppy-harvest, croppy-water, croppy-fertilize, croppy-cook)
    use-custom-flags: true

  papi:
    # Enables PlaceholderAPI expansion for live telemetry
    enabled: true
    # Maximum raycast distance in blocks to resolve targeted crops and workstations
    raycast-distance: 5

  vault:
    # Enables currency operations and transaction validations via Vault
    enabled: true
```

* **`worldguard`**: Enforces land claims and registers custom agricultural/cooking state flags.
* **`papi`**: Provides live crop, soil, workstation, and player compendium placeholders with configurable raycast range.
* **`vault`**: Connects to the server's economy provider for recipe and cooking transactions.

For in-depth details on flags, placeholders, and adapter behavior, see the [Third-Party Integrations Guide](./integrations.md).

---

## 5. Crafting Table Recipes (`crafting`)

Croppy supports native Bukkit crafting table recipes for cookware, tools, watering cans, and fertilizers:

```yaml
crafting:
  # Automatically discover all registered custom recipes in the player's recipe book upon joining
  auto-discover-recipes: true
```

* **`auto-discover-recipes`**: When set to `true`, players automatically unlock all registered Croppy crafting table recipes in their recipe book upon joining the server. For crafting recipe declarations, see the [Crafting Table Recipes Guide](./crafting-recipes.md).

---

## 6. Custom Items & Inspection Tool Customization

The Crop Inspector tool (`croppy:inspection_tool`) is defined through Croppy's centralized **Item Registry** in `plugins/Croppy/items/tools/inspector.yml` (using base material `minecraft:stick` to avoid conflicts with administrative navigation wands). To customize its material, display name, item model, or lore, edit its YAML definition directly or consult the [Items & Tools Guide](./items-and-tools.md).
