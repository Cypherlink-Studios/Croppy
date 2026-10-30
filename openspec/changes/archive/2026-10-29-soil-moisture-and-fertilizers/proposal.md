# Proposal

## Why

Currently, crop growth in Croppy depends solely on passive environmental proximity to light and static Minecraft water blocks. Farming feels passive and lacks active player engagement, strategic soil preparation, and meaningful tool progression. Introducing active watering mechanics (watering cans, daily hydration, rain exposure) and declarative fertilizers (speed, moisture retention, yield quality) with configurable unwatered growth policies transforms agriculture into an engaging, rewarded gameplay loop while retaining high performance.

## What Changes

- **Watering Cans & Hydration**: Adds multi-tiered watering cans (Copper, Iron, Gold, Diamond, Netherite) with varying water capacity, area-of-effect watering (1x1, 1x3, 3x3, 5x5), refill mechanics from water sources and cauldrons, water splash particles, and bucket/splash audio feedback.
- **Discrete Daily Hydration Model**: Tracks soil moisture via absolute world day (`lastWateredDay`). Crops are either watered or dry for the day. Naturally hydrates exposed crops during rain storms in precipitation-capable biomes when not sheltered by roofs or greenhouses.
- **Unwatered Policy System**: Introduces configurable policies (`PAUSE`, `SLOW`, `DECAY_PROGRESS`, `WITHER`) applied when crops lack daily hydration, configurable globally and per species.
- **Declarative Fertilizers**: Introduces YAML-based fertilizer definitions (`SPEED`, `RETAINING`, `QUALITY`) with item bindings. Enforces a 1-fertilizer-per-crop limit that persists across multi-harvest regrowth cycles, supported by a 30-second transient cache for fertilizing tilled soil prior to planting.
- **Persistence Codec V2 & Schema Migration**: Upgrades `CropBinaryCodec` to Version 2 to store `lastWateredDay` and `fertilizerKey` with backwards compatibility for V1, and adds columns to SQLite storage.
- **Inspection & Admin Tooling**: Updates `/croppy info` to show daily hydration status, consecutive dry days, active fertilizer, and applied policy; adds administrative commands to hydrate and fertilize crops.

## Capabilities

### New Capabilities
- `watering-mechanics`: Covers watering tools (tiered watering cans, durability/capacity, AoE watering), manual watering interactions, rain-based sky exposure hydration, and visual/audio soil feedback.
- `fertilizer-nutrients`: Covers declarative YAML fertilizer definitions, application mechanics (single fertilizer constraint, 30s pre-plant soil buffer, lifecycle persistence across harvests), and effect execution (Speed-Gro growth modifier, Water Retaining soil day-change roll, Quality booster).

### Modified Capabilities
- `growth-simulation`: Incorporates daily soil hydration verification and the `UnwateredPolicy` pipeline into the growth engine and modifier chain.
- `crop-persistence`: Updates Chunk PDC binary serialization to Version 2 and migrates SQLite database tables to persist `lastWateredDay` and `fertilizerKey`.

## Impact

- **API (`croppy-api`)**: Expands `CropInstance` with `lastWateredDay` and `fertilizerKey`; introduces `UnwateredPolicy` and `Fertilizer` abstractions.
- **Core (`croppy-core`)**: Updates `CropBinaryCodec` to V2; integrates `FertilizerGrowthModifier` and `UnwateredPolicy` checks in `GrowthEngine`.
- **Platform (`croppy-paper`)**: Adds watering can and fertilizer item listeners, rain ticker/listeners, bed sleep / time skip handling, SQLite schema updates, and updated inspection card.
- **Configuration & Storage**: Backwards-compatible binary deserialization prevents data loss in existing worlds. No breaking changes to existing species YAML formats.
