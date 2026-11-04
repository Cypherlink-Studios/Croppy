---
title: Custom Items & Tools
description: Catalog of Croppy's custom items, agricultural tools, PDC data persistence, and item registration format.
sidebar:
  order: 18
---

# Custom Items & Agricultural Tools

Croppy provides a unified item framework that bridges vanilla items with custom behavioral items. All items are identified by semantic keys (`croppy:<item_name>`), support modern Minecraft 1.21.4+ `item_model` Data Components, CustomModelData integer values, and embed durable metadata into Minecraft's native **PersistentDataContainer (PDC)**.

---

## 1. Agricultural Tools Catalog

Croppy equips farmers and chefs with specialized tools:

```text
+-----------------------------------------------------------------------------+
|                          AGRICULTURAL & CHEF TOOLS                          |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [SOIL & CROP INSPECTOR] (croppy:inspector)                                 |
|  - Base Material: SPYGLASS                                                  |
|  - Function: Right-click any farmland or growing crop to display its live   |
|    inspection report (species, stage, progress %, light, moisture, NPK).    |
|                                                                             |
|  [CULINARY THERMOMETER] (croppy:thermometer)                                |
|  - Base Material: BLAZE_ROD                                                 |
|  - Function: Right-click any cooking station or stove to read its exact     |
|    surface temperature in Celsius and active thermal zone.                  |
|                                                                             |
|  [RECIPE BOOK] (croppy:recipe_book)                                         |
|  - Base Material: BOOK                                                      |
|  - Function: Right-click in hand to instantly open the interactive         |
|    Culinary Compendium cookbook GUI.                                        |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Item Categories

Items are organized under semantic subdirectories inside `plugins/Croppy/items/<category>/`:

| Category | Typical Contents | Examples |
|---|---|---|
| `seeds` | Plantable crop seeds linking to species definitions | `croppy:wheat_seeds`, `croppy:tomato_seeds` |
| `tools` | Diagnostic and operational implements | `croppy:inspector`, `croppy:thermometer` |
| `watering_cans` | Hydration equipment with capacities and AoE widths | `croppy:copper_can`, `croppy:gold_can` |
| `fertilizers` | Nutrient enhancements (Speed, Quality, Retaining) | `croppy:speed_gro`, `croppy:retaining_soil` |
| `stations` | Placeable physical block workstation items | `croppy:cooking_pot`, `croppy:skillet`, `croppy:stove` |
| `ingredients` | Harvest produce, processed meats, broths & dishes | `croppy:tomato`, `croppy:tomato_soup` |
| `misc` | Encyclopedias, cooking manuals, and general items | `croppy:recipe_book`, `croppy:burnt_food` |

---

## 3. Item YAML Schema Specification

Every item configuration is parsed by `YamlItemLoader`. Here is a complete reference example:

```yaml
id: "croppy:speed_gro"
category: "fertilizers"

# Display title supporting MiniMessage gradient formatting
name: "<gradient:#55ff55:#22aa22>Speed-Gro Fertilizer</gradient>"

# Lore lines displayed on the item tooltip
lore:
  - "<gray>Enriches soil with active nitrates.</gray>"
  - "<gold>+25% Crop Growth Rate</gold>"

# Base vanilla Minecraft material
material: "BONE_MEAL"

# Minecraft 1.21.4+ Modern Data Component (assets/<ns>/items/*.json)
item_model: "croppy:speed_gro"

# Legacy or fallback integer CustomModelData
custom_model_data: 1001

# Custom skin texture for player heads (Base64 payload or direct Mojang URL)
# texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv..."
# Alias: skull-texture

# Visual and durability attributes
unbreakable: true
glow: false
flags:
  - "HIDE_ATTRIBUTES"
  - "HIDE_ADDITIONAL_TOOLTIP"

# Custom PersistentDataContainer key-value tags
pdc:
  type: "SPEED"
  speed_multiplier: "1.25"
```

### Field Reference Table

| Field Path | Type | Required | Description |
|---|---|---|---|
| `id` | String (`Key`) | **Yes** | Unique identifier in `<namespace>:<value>` format. |
| `category` | String | **Yes** | Directory category (e.g. `seeds`, `tools`, `ingredients`). |
| `name` | String | **Yes** | MiniMessage formatted item display name. |
| `material` | String | **Yes** | Bukkit/Minecraft Material name (e.g., `BONE_MEAL`, `SPYGLASS`). |
| `lore` | List of Strings | No | Multiline tooltip lore with MiniMessage formatting. |
| `item_model` | String (`Key`)| No | Namespaced model path for 1.21.4+ native Data Component `item_model`. |
| `custom_model_data` | Integer | No | Numeric integer for CustomModelData overrides. |
| `texture` | String | No | Custom skin texture for `PLAYER_HEAD` items. Accepts Base64 encoded JSON or direct Mojang URL (`http://textures.minecraft.net/texture/...`). Alias: `skull-texture`. |
| `unbreakable` | Boolean | No | Flags tool as unbreakable. |
| `glow` | Boolean | No | Applies an enchantment glint without showing enchantments. |
| `flags` | List of Strings | No | ItemFlags applied to hide vanilla tooltips (e.g., `HIDE_ATTRIBUTES`). |
| `pdc` | Map<String, String> | No | Custom persistent String tags written to the item's `PersistentDataContainer`. |

---

## 4. Custom Head Textures (Base64 & Mojang URLs)

Items configured with `material: PLAYER_HEAD` (or `minecraft:player_head`) can specify custom head textures using the `texture` property (or its alias `skull-texture`).

Croppy automatically resolves both direct Mojang skin URLs and Base64 encoded JSON texture payloads:

### Base64 Value Example
```yaml
id: "croppy:cutting_board"
category: "stations"
name: "<yellow>Cutting Board</yellow>"
material: "PLAYER_HEAD"
texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJkYTVmMjc3YzhlMmM2NWVkN2JhOTJkMWIxY2M5YTc5YTRlOTQ1OWVkNWU0ZTk0YjE4ZmVhYTk3N2FjIn19fQ=="
```

### Direct Mojang URL Example
```yaml
id: "croppy:cutting_board"
category: "stations"
name: "<yellow>Cutting Board</yellow>"
material: "PLAYER_HEAD"
texture: "http://textures.minecraft.net/texture/4bda5f277c8e2c65ed7ba92d1b1cc9a79a4e9459ed5e4e94b18feaa977ac"
```

### Deterministic UUID Profile Generation
To prevent network latency and avoid skin collisions on Paper, Croppy constructs a `PlayerProfile` with a deterministic UUID derived from the texture hash via name-based UUID generation (MD5). When this item is placed in the world as a physical station base block, the target block's `Skull` tile state is automatically updated with the profile.

---

## 5. Persistent Data Container (PDC) Architecture

Every Croppy item embeds persistent tags within its PDC:
* **`croppy:item_id`**: The immutable semantic identifier linking the physical item to its registry definition.
* **`croppy:category`**: Semantic classification category of the item.
* **`croppy:water_level` / `croppy:capacity`**: Tracks remaining water charges on watering cans.
* **`croppy:fertilizer_type` / `croppy:speed_multiplier`**: Tracks applied fertilizer properties.

Because data is stored natively inside Minecraft's NBT/PDC structures, items survive server restarts, player inventory changes, and container movements without desynchronization.
