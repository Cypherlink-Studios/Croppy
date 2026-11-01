# Proposal

## Why

Currently, culinary cookware stations (such as `croppy:cooking_pot` and `croppy:frying_pan`) rely on static vanilla Minecraft blocks beneath them (e.g. campfires at 300°C or fire at 500°C) to receive thermal energy. Server administrators and players have no mechanism to build dedicated, adjustable heat source stations (like rustic wood stoves, modern ranges, or alchemical hearths) capable of fine-tuning exact temperatures, throttling fuel consumption, or limiting maximum heat generation.

Introducing variable heat source stations provides players with precise culinary control (via GUI increment buttons and chat input), modular fuel consumption (supporting vanilla fuels now and extensible custom items like ExecutableItems later), and integrated binary persistence.

## What Changes

- **Station Heat Source Component (`croppy-api` & `croppy-core`)**: Introduce `HeatSourceComponent` and `DefaultHeatSourceComponent` representing active heat generation, target and output temperatures, fuel consumption, and configurable operational limits.
- **Extensible Fuel Registry & Providers**: Introduce `FuelProvider` and `FuelProperties` port in `croppy-api` and `DefaultFuelRegistry` in `croppy-core` to calculate burn ticks, maximum achievable temperatures, and fuel efficiency, supporting vanilla fuels and future custom item providers (e.g., ExecutableItems).
- **Dynamic Fuel Burn Scaling**: Scale fuel consumption rates based on target operating temperature relative to station limits, consuming fuel faster at sear/scorch levels than at low simmer levels.
- **Configurable Station Heat Limits**: Add station definition properties for `min_temperature`, `max_temperature`, `heating_rate`, `cooling_rate`, and `fuel_burn_multiplier` to support both rustic stoves and extreme alchemical burners.
- **Thermal Propagation Priority**: Update `PaperThermalEvaluator` to check for active `HeatSourceComponent` stations at `(x, y - 1, z)` before falling back to vanilla block evaluation.
- **Station Control Menu & Chat Input**: Enhance station menu interactions with +/- degree increment buttons, power toggles, thermostat displays, and a chat prompt service (`ChatPromptService`) capturing numerical temperature input via `AsyncChatEvent` with auto-reopening GUI.
- **In-World Thermal Particles**: Spawn colored `DUST` particles on active heat source stations reflecting current temperature levels (amber warm glow -> incandescent red -> bright cyan/blue flame).
- **Binary Persistence Upgrade**: Bump `StationBinaryCodec` to version 3 to serialize and restore `HeatSourceComponent` across server restarts and chunk unloads.

## Capabilities

### New Capabilities
- `heat-generation`: Defines station-driven thermal energy generation, fuel consumption scaling, exact temperature regulation, chat input prompts, and operational thermal limits.

### Modified Capabilities
- `culinary-thermal-system`: Modifies thermal detection to prioritize active `HeatSourceComponent` stations directly beneath cookware (`y - 1`).
- `crafting-stations`: Modifies station lifecycle and persistence to support `HeatSourceComponent` attachment, configuration properties, and binary codec v3 serialization.

## Impact

- `croppy-api`: New contracts `HeatSourceComponent`, `FuelProvider`, and `FuelProperties`.
- `croppy-core`: `DefaultHeatSourceComponent`, `DefaultFuelRegistry`, and version 3 upgrade in `StationBinaryCodec`.
- `croppy-paper`: `PaperThermalEvaluator` station query hook, `ChatPromptService` listener, dynamic `DUST` particle rendering in `PaperStationTicker`, and menu action handlers.
- Compatibility: Existing stations remain backwards-compatible with `StationBinaryCodec` version 2 data.
