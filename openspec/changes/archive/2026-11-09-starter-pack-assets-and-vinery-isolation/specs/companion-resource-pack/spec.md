# Spec Delta

## MODIFIED Requirements

### Requirement: Modern Item Model Definitions
The resource pack SHALL declare custom item models inside `assets/croppy/items/<item>.json` utilizing modern dispatch trees (`minecraft:model`, `minecraft:select`, `minecraft:range_dispatch`), providing synchronized item definitions for neutral vanilla-tiered tools, crops, and workstations without orphaned, unreferenced, or non-neutral assets. Item models MUST NOT declare legacy `overrides` arrays in `models/item/`.

#### Scenario: Resolving item definition for custom items
- **WHEN** a player holds an item stack whose `item_model` component points to a registered item definition
- **THEN** the client resolves the model geometry via `assets/croppy/items/<item>.json` matching the valid item model target

#### Scenario: Validating model definitions for watering can tiers
- **WHEN** the resource pack compiles item definitions for watering cans
- **THEN** item models for native Minecraft material tiers (`wooden_can`, `copper_can`, `iron_can`, `golden_can`, `diamond_can`, `netherite_can`) SHALL exist under `assets/croppy/items/`

## ADDED Requirements

### Requirement: Neutral Starter Pack Assets and Vinery Isolation
The companion resource pack SHALL isolate viticulture and grape-specific assets (`plant/grape`, `purple_grapes`, `green_grapes`) into `.internal/resourcepack/` to maintain aesthetic neutrality, and SHALL supply dedicated textures and model geometries for all base starter-pack items (watering cans, fertilizers, tools, seeds, harvested produce, and miscellaneous culinary items) under `assets/croppy/models/item/` and `assets/croppy/textures/item/`.

#### Scenario: Isolating vinery assets from core pack
- **WHEN** the companion resource pack is inspected for theme neutrality
- **THEN** grape plant stages and grape item assets are relocated to `.internal/resourcepack/vinery/` and are absent from `resourcepack/assets/croppy/`

#### Scenario: Verifying dedicated custom assets for base items
- **WHEN** a client renders any registered base starter-pack item
- **THEN** the client resolves a custom pixel-art texture and model under `croppy:*` rather than falling back to vanilla hoes, sticks, or coal
