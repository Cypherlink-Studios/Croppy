# Proposal: Companion Resource Pack Scaffold

## Why

With Croppy 0.3.0 introducing native item model Data Component resolution (`item_model`) and PacketEvents virtual displays for cooking stations, custom items, tools, and UI menus require an official Minecraft 1.21.8 (`pack_format: 64`) companion resource pack. Establishing this structured asset workspace within the repository ensures seamless in-game visual rendering, borderless chest GUI layouts, and reproducible asset packaging without missing textures or legacy format regressions.

## What Changes

- Initialize an official companion resource pack directory under `resourcepack/`.
- Author canonical metadata in `resourcepack/pack.mcmeta` with integer `pack_format: 64` and `supported_formats: {"min_inclusive": 63, "max_inclusive": 64}` conforming to Minecraft Java 1.21.8.
- Establish the modern namespaced asset directory structure under `resourcepack/assets/croppy/` (`items/`, `models/`, `textures/`, `font/`, `sounds/`).
- Create initial modern item definition files in `resourcepack/assets/croppy/items/` corresponding to registered Croppy items (`recipe_book.json`, `copper_can.json`, `gold_can.json`, `iridium_can.json`, `rusty_can.json`, `silver_can.json`, `cooking_pot.json`, `skillet.json`, `stove.json`, `inspector.json`, `thermometer.json`, `burnt_food.json`).
- Configure Unicode font providers in `resourcepack/assets/croppy/font/default.json` including powers-of-two negative advances (`\uF801` through `\uF880`) for borderless menu rendering in Chest GUIs.
- Enforce strict JSON and naming validation using `mc-rp-scaffolder-1218`'s `audit_pack_1218.py`.

## Capabilities

### New Capabilities
- `companion-resource-pack`: Manages the canonical Minecraft 1.21.8 Resource Pack structure, format 64 metadata, modern item model definitions, negative-space font providers, and asset verification gates.

### Modified Capabilities
None.

## Impact

- Repository Structure: Adds `resourcepack/` directory at the project root.
- Server Plugin Runtime: Does not alter backend Java 21 compilation or plugin JAR size; provides the companion client assets needed for full visual fidelity.
- Testing & Quality: Establishes a zero-error quality gate for asset JSONs using `audit_pack_1218.py`.
