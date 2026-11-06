# Proposal: Fix Crop Harvest and Seed Drops for Custom Items

## Why

When players right-click to harvest mature custom crops (such as grapes, tomatoes, or rice) or break crops in the world, no harvest yields or seed items are dropped, yet the plant transitions its developmental state (e.g. reverting to its regrowth stage or being destroyed). This occurs because `VirtualInteractionPacketListener` and `CropBreakListener` attempt to resolve drop item strings exclusively using Bukkit's `Material.matchMaterial(itemId)`, which returns `null` for all custom items registered in Croppy's `ItemRegistry` (e.g. `croppy:grapes`, `croppy:grape_seeds`, `croppy:tomato`, `croppy:rice_panicle`). Consequently, players receive nothing while losing crop progress or planted seed investments.

## What Changes

- **Unified Item Resolution in `ItemFactory`**: Introduce a centralized method in `ItemFactory` (`resolveItemStack(String itemId, int amount)`) that attempts resolution against `ItemRegistry` first (constructing full custom `ItemStack`s with PDC metadata, display names, lore, and custom model data), falling back to native Bukkit `Material` resolution for vanilla items (e.g. `minecraft:wheat`, `minecraft:oak_fence`).
- **`ItemFactory` Dependency Injection**: Inject `ItemFactory` into `VirtualInteractionPacketListener` and `CropBreakListener` in `CroppyPlugin`.
- **Custom Harvest Yields and Seed Drop Resolution**:
  - Update `VirtualInteractionPacketListener.performHarvest()` to resolve harvest yields via `ItemFactory`.
  - Update `VirtualInteractionPacketListener.performDismantle()` to resolve seed drops via `ItemFactory`.
  - Update `VirtualInteractionPacketListener` action `DROP_ITEM` to support custom items via `ItemFactory`.
  - Update `CropBreakListener.onBlockBreak()` to resolve both mature harvest yields and immature seed drops via `ItemFactory`.
- **Unit and Integration Test Coverage**: Add unit tests in `croppy-paper` verifying that custom items in `ItemRegistry` and vanilla materials are correctly resolved into Bukkit `ItemStack`s during crop harvest and dismantling.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `crop-lifecycle`: Require harvest yield and seed drop calculations to resolve custom items registered in `ItemRegistry` in addition to vanilla Minecraft materials.
- `interaction-engine`: Require `HARVEST_CROP` and `DROP_ITEM` action handlers to resolve custom `ItemRegistry` items when spawning drops.

## Impact

- **Affected Components**:
  - `croppy-paper`: `ItemFactory`, `VirtualInteractionPacketListener`, `CropBreakListener`, `CroppyPlugin`.
  - No breaking API changes in `croppy-api` or `croppy-core`.
  - Full backwards compatibility with existing YAML configurations (`crops/*.yml`, `interactions/crops/*.yml`).
