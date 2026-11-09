---
title: Third-Party Integrations
description: Complete guide to Croppy's modular integrations with WorldGuard, PlaceholderAPI, and Vault.
sidebar:
  order: 20
---

# Third-Party Integrations

Croppy features an isolated, modular integrations subsystem (`croppy-integrations`) designed around **Hexagonal Architecture (Ports and Adapters)**. Third-party libraries never contaminate domain simulation modules (`croppy-core` or `croppy-api`), ensuring 100% stability, zero hard dependencies, and graceful fallback behavior when plugins are missing or disabled.

---

## 1. Architectural Model

```text
+-------------------------------------------------------------+
|                         croppy-api                          |
|             Pure Java 21 Ports (Zero Dependencies)          |
|  * ProtectionPort & ProtectionAction                        |
|  * EconomyPort                                              |
+------------------------------+------------------------------+
                               | implements
+------------------------------v------------------------------+
|                     croppy-integrations                     |
|  * common:      IntegrationManager, CroppyIntegration       |
|  * worldguard:  WorldGuardProtectionAdapter, WorldGuardFlags |
|  * papi:        CroppyPlaceholderExpansion                  |
|  * vault:       VaultEconomyAdapter                         |
+------------------------------+------------------------------+
                               | shaded & wired
+------------------------------v------------------------------+
|                        croppy-paper                         |
|  * Event Listeners (CropPlantListener, WateringListener...) |
|  * Virtual Packet Interceptors (PacketEvents Displays)      |
+-------------------------------------------------------------+
```

When a supported plugin is detected on the Paper server, its integration module automatically activates. If the plugin is absent or disabled in `config.yml`, Croppy transparently falls back to safe `NoOp` adapters without logging runtime exceptions or interrupting simulation loops.

---

## 2. Configuration (`config.yml`)

The integrations subsystem is controlled through the `integrations` block in `plugins/Croppy/config.yml`:

```yaml
# ==============================================================================
# Third-Party Plugin Integrations
# ==============================================================================
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

---

## 3. WorldGuard Integration (`croppy-integrations:worldguard`)

Croppy integrates deeply with **WorldGuard 7.0+** to enforce region boundaries and land claims across all agricultural and culinary activities.

### Custom WorldGuard Flags

When `use-custom-flags: true`, Croppy registers 5 custom state flags during server startup (`onLoad` lifecycle):

| Flag | Description | Default State |
|---|---|---|
| `croppy-plant` | Controls permission to plant crop seeds into tilled soil or trellises. | `ALLOW` |
| `croppy-harvest` | Controls permission to harvest mature crops (both physical and virtual 3D displays). | `ALLOW` |
| `croppy-water` | Controls permission to use watering cans on crops and farmland. | `ALLOW` |
| `croppy-fertilize` | Controls permission to apply organic fertilizers to farmland. | `ALLOW` |
| `croppy-cook` | Controls permission to interact with workstations, insert ingredients, and retrieve cooked dishes. | `ALLOW` |

### Fallback Protection & Bypass
* **Custom Flag Priority**: If a region defines a specific `croppy-*` flag (e.g. `/rg flag spawn croppy-harvest deny`), that value takes precedence.
* **Standard WorldGuard Fallback**: If custom flags are unset or disabled, actions fall back to WorldGuard's standard flags:
  * `BUILD` flag for planting seeds, applying fertilizer, and placing/dismantling stations.
  * `INTERACT` flag for watering cans, harvesting crops, and cooking interactions.
* **Admin Bypass**: Players with WorldGuard region bypass permissions (`worldguard.region.bypass.<world>`) or full server bypass bypass all protection checks.

### Virtual Display & Packet Protection
Croppy's virtual 3D block displays and workstations exist as client-side packet projections via **PacketEvents**. Unauthorized players clicking virtual hitboxes have their interaction packets intercepted and denied by `ProtectionPort`, preventing packet-level claim exploitation.

---

## 4. PlaceholderAPI Integration (`croppy-integrations:papi`)

Croppy registers a native PlaceholderAPI expansion under the `%croppy_*%` identifier, providing real-time telemetry for HUD overlays, action bars, scoreboards, and custom menus.

### Target Raycasting
Placeholders prefixed with `%croppy_target_*%` dynamically cast a sightline from the player up to `raycast-distance` blocks (default: `5`). The raycast inspects targeted crops, stations, and farmland directly or beneath active crops.

### Supported Placeholders

#### Global System Telemetry
| Placeholder | Description | Example Output |
|---|---|---|
| `%croppy_version%` | Active Croppy plugin version string. | `0.5.16` |
| `%croppy_total_active_crops%` | Total tracked crop instances currently loaded on the server. | `128` |
| `%croppy_total_active_stations%` | Total registered cooking stations currently loaded on the server. | `16` |

#### Targeted Crop Telemetry
| Placeholder | Description | Example Output |
|---|---|---|
| `%croppy_target_is_crop%` | Whether the player is directly targeting a valid Croppy crop. | `true` |
| `%croppy_target_crop_species%` | Canonical namespaced key of the targeted crop species. | `croppy:grapes` |
| `%croppy_target_crop_species_name%` | Formatted human-readable display name of the crop. | `Sweet Grapes` |
| `%croppy_target_crop_stage%` | Current developmental growth stage index (0-indexed). | `3` |
| `%croppy_target_crop_max_stage%` | Final developmental stage index (0-indexed, `total_stages - 1`). | `4` |
| `%croppy_target_crop_total_stages%` | Total number of growth stages for the species. | `5` |
| `%croppy_target_crop_progress%` | Growth progress percentage towards the next stage. | `74.5%` |
| `%croppy_target_crop_is_watered%` | Whether the targeted crop has received hydration today. | `true` |
| `%croppy_target_crop_fertilizer%` | Namespaced key of active fertilizer applied to the crop, or `none`. | `croppy:growth_fertilizer` |

#### Targeted Soil Telemetry
| Placeholder | Description | Example Output |
|---|---|---|
| `%croppy_target_soil_is_farmland%` | Whether the targeted block or block beneath the crop is farmland. | `true` |
| `%croppy_target_soil_water%` | Raw vanilla moisture level of the farmland (`0` to `7`). | `6` |
| `%croppy_target_soil_moisture%` | Farmland moisture saturation percentage (`0%` to `100%`). | `86%` |

#### Targeted Workstation Telemetry
| Placeholder | Description | Example Output |
|---|---|---|
| `%croppy_target_is_station%` | Whether the player is directly targeting a valid cooking workstation. | `true` |
| `%croppy_target_station_name%` | Namespaced key or type of the targeted workstation (alias of `_type`). | `croppy:skillet` |
| `%croppy_target_station_type%` | Canonical namespaced key of the targeted workstation. | `croppy:skillet` |
| `%croppy_target_station_temperature%` | Current operating temperature of the station formatted to one decimal. | `185.0°C` |
| `%croppy_target_station_is_cooking%` | Whether the station has an active recipe or cooking process running. | `true` |
| `%croppy_target_station_cooking_progress%` | Completion percentage of active multi-phase cooking process. | `62.0%` |

#### Player Culinary Compendium
| Placeholder | Description | Example Output |
|---|---|---|
| `%croppy_compendium_discovered%` | Number of culinary recipes unlocked by the player. | `14` |
| `%croppy_compendium_discovered_recipes%` | Descriptive alias for `%croppy_compendium_discovered%`. | `14` |
| `%croppy_compendium_total%` | Total registered culinary recipes on the server. | `42` |
| `%croppy_compendium_total_recipes%` | Descriptive alias for `%croppy_compendium_total%`. | `42` |
| `%croppy_compendium_percentage%` | Percentage of compendium mastery formatted to one decimal. | `33.3%` |
| `%croppy_compendium_completion_percentage%` | Descriptive alias for `%croppy_compendium_percentage%`. | `33.3%` |
| `%croppy_compendium_is_unlocked_<recipe_key>%` | Whether the specified recipe is discovered by the player. | `true` |

---

## 5. Vault Integration (`croppy-integrations:vault`)

Croppy provides a unified financial bridge via **Vault 1.7+** through the pure domain contract `EconomyPort`:

* **Transactional Safety**: Balance verifications, fund deductions, and payouts occur through atomic transactions.
* **Locale-Aware Formatting**: Financial values formatted via `EconomyPort.format(amount)` format monetary amounts cleanly (e.g. `$100.00`).
* **Graceful Degradation**: If Vault or an economy provider (EssentialsX, EconomyShopGUI, etc.) is missing, Croppy's `NoOpEconomyAdapter` denies paid operations safely without throwing `NullPointerException` or breaking recipe discovery.
