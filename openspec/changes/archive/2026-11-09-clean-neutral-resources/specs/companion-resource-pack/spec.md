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
