# Proposal: Clean Neutral Resources

## Why

Croppy is intended to serve as a versatile, modular agriculture, processing, and cooking simulation foundation for Minecraft Paper servers. However, its bundled base resources currently incorporate theme-specific tropes (primarily Stardew Valley mechanics such as `rusty`, `silver`, and `iridium` watering cans, and trademarked names like `speed_gro`), an unreferenced modern fast-food asset (`hamburger.png`), hardcoded Spanish labels, and broken model references.

To serve as a truly neutral, theme-agnostic baseline for Declarative Content Packs and Java Addons, the core plugin resources and companion resource pack must be thoroughly cleaned, adopting native Minecraft material tiers, neutral agronomy nomenclature, and Standard English localization.

## What Changes

- **Vanilla-Aligned Watering Cans**: Replace the Stardew progression (`rusty`, `copper`, `silver`, `gold`, `iridium`) with Minecraft vanilla tiers (`wooden`, `copper`, `iron`, `golden`, `diamond`, `netherite`) with matching capacities and area-of-effect parameters.
- **Neutral Fertilizer Nomenclature**: Rename brand-like identifiers (`speed_gro` -> `growth_fertilizer`, `retaining_soil` -> `hydrating_fertilizer`, `quality_fertilizer` -> retained) and neutralize descriptions.
- **Resource Pack Asset Cleanup**:
  - Remove orphan and modern assets (`textures/item/hamburger.png`).
  - Synchronize item model definitions in `assets/croppy/items/` for new vanilla watering cans and remove deprecated ones.
  - Sincronize grape harvest definition with `croppy:grapes`.
  - Establish standardized stage blueprints and item definitions for 3D `tomato` and `rice` plants ready for external Blockbench modeling.
- **Language & Lore Neutralization**:
  - Convert all Spanish titles, tool names, and lores (`carrot.yml`, `inspector.yml`, watering cans, fertilizers) to Standard English according to project rules.
  - Purge fantasy/mystical flavor lore.
- **Unit Test Synchronizations**: Update existing test suites asserting old IDs (`speed_gro`, `retaining_soil`, `copper_can`, etc.) to match the new neutral identifiers.

## Capabilities

### Modified Capabilities
- `companion-resource-pack`: Update companion resource pack item model declarations and asset tree compliance to reflect vanilla-tiered tools, standardized crop stage item models, and deletion of orphaned assets.
- `watering-mechanics`: Update watering can tier definitions to align strictly with native Minecraft material tiers (`wooden`, `copper`, `iron`, `golden`, `diamond`, `netherite`).
- `fertilizer-nutrients`: Update declarative fertilizer definitions to use neutral agronomical nomenclature (`growth_fertilizer`, `hydrating_fertilizer`, `quality_fertilizer`).

## Impact

- **Configuration & Resources**: YAML files in `croppy-paper/src/main/resources/{items,crops}/` and assets in `resourcepack/` are updated.
- **Code & Registries**: No core API or architectural changes; identifiers (`Key`) in bundled resources and tests are updated.
- **Downstream Packs & Addons**: Addons and content packs gain a completely neutral canvas without conflicting thematic assumptions.
