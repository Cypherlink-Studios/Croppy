# Design: Coffee Station Display Models Alignment

## Context

See `proposal.md` for motivation.

Croppy uses client-side packet virtual displays via PacketEvents to render station models in the world. When a station visual defines an item-based `base_model`, `StationViewerTracker` queries `ItemFactory.getItemRegistry().get(itemKey)` to build the corresponding `ItemStack`. If found, `ItemFactory.createItemStack` sets the modern Minecraft 1.21.8 `item_model` Data Component on the item meta, matching the resource pack definition path. If not found, it falls back to matching a vanilla Material, and ultimately to `Material.PAPER`.

## Goals / Non-Goals

**Goals:**
- Correct station `base_model.item` identifiers to reference registered domain item IDs (`croppy_coffee:<id>`).
- Retain the `assets/croppy_coffee/items/block/` directory structure for station block items.
- Align `item-model` attributes in `items/stations/*.yml` to match `croppy_coffee:block/<id>`.
- Fix the `expresso` -> `espresso` typographical errors in asset files and model targets.
- Align seed and ingredient item-models to `croppy_coffee:item/<id>`.
- Provide automated regression test coverage in `CoffeeResourcesIntegrityTest`.

**Non-Goals:**
- Restructuring the core PacketEvents station rendering pipeline (which functions as designed).
- Migrating station items from `items/block/` to root `items/` (user explicitly opted to keep `block/`).

## Decisions

### 1. Maintain `block/` Subdirectory in Resource Pack Assets
- **Choice**: Keep `assets/croppy_coffee/items/block/*.json`.
- **Rationale**: Groups station block items neatly separate from inventory items (`item/`). Minecraft 1.21.8 fully supports nested paths in the `item_model` component (e.g. `namespace:path/to/item`).
- **Alternative Considered**: Flattener like `croppy:stove` directly under `assets/croppy/items/stove.json`. Rejected per user choice.

### 2. Station Definition References Domain Item ID
- **Choice**: `stations/*.yml` uses `item: "croppy_coffee:<station_id>"`.
- **Rationale**: In Croppy's architecture, `base_model.item` is a domain `Key` resolved against `ItemRegistry`, not a direct resource pack path. The item definition holds the material, name, lore, and `item-model` component.

### 3. Automated Integrity Assertion in Addon Test Suite
- **Choice**: Expand `CoffeeResourcesIntegrityTest` to load station and item YAMLs and verify that:
  1. Every station's `base_model.item` is present in the items folder.
  2. Every item's `item-model` points to an existing `.json` file in `src/main/resources/assets/<namespace>/items/`.
  3. Every item model JSON references an existing model in `models/`.
- **Rationale**: Prevents future regressions and typos during addon development.

## Risks / Trade-offs

- **[Risk] Existing placed stations in worlds might retain previous state** → Station display entities are purely packet-based (virtual) and reconstructed dynamically when players enter tracking range. Reloading the plugin or restarting the server updates client visuals immediately without requiring block replacement.
