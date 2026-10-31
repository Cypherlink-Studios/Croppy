# Proposal

## Why

Croppy currently provides fundamental station registration and sequential process simulation, but lacks a dynamic culinary cooking loop inspired by Farmer's Delight. Cooking stations like cooking pots and skillets require thermal mechanics (heat sources beneath), tactile multi-sensory feedback (boiling liquids, sizzling sounds, smoke particles), data-driven interaction routing without hardcoding, and a flexible rule engine to handle penalties such as overcooking and thermal burns.

Implementing these capabilities now empowers server administrators and culinary creators to build deep, immersive cooking experiences via simple YAML configurations while preserving Croppy's strict architectural boundaries and performance guarantees.

## What Changes

- **Culinary Cookware & Stations**: Introduce dedicated cooking cookware definitions (`croppy:cooking_pot` and `croppy:skillet`) with YAML loading from `content/stations/` and hybrid interaction modes.
- **Thermal & Heat Source System**: Implement a lightweight, stable thermal engine that detects active heat sources beneath cookware (Campfires, Furnaces, Smokers, Fire, Magma/Lava), maintains station temperature states, and categorizes operational thermal zones (`COLD`, `WARM`, `SIMMER`, `BOIL`, `SEAR`, `SCORCH`).
- **Culinary Thermometer**: Add the `croppy:thermometer` tool with configurable durability (or infinite), rendering MiniMessage action bar temperature readouts and mechanical compass click audio feedback exclusively on culinary stations.
- **Declarative Interaction Engine**: Add `InteractionDefinition` and an accompanying registry loading from `content/interactions/`, allowing data-driven mapping of player clicks (sneak, empty hand, specific tool/item, conditions) to discrete actions (`OPEN_GUI`, `SHOW_TEMPERATURE`, `INSERT_INPUT`, `SERVE_OUTPUT`, `PLAY_SOUND`, `EMIT_PARTICLES`).
- **Configurable Rule Engine**: Add `RuleDefinition` and registry loading from `content/rules/`, evaluating Pratt AST condition expressions against station and process state triggers to execute consequences like output burning (`croppy:burnt_food`), quality degradation, player burn injuries on scorching contact, particles, and audio alerts.
- **Multi-Sensory Display Feedback**: Implement `ItemDisplay` positioning (liquid inside pot, flat items on skillet surface) and ambient particle/sound emitters (bubbling, sizzling, steam, smoke) in `PaperStationTicker`.

## Capabilities

### New Capabilities
- `culinary-thermal-system`: Manages thermal states, heat source detection from the supporting block (`y - 1`), thermal zones (COLD, WARM, SIMMER, BOIL, SEAR, SCORCH), and the culinary thermometer item (`croppy:thermometer`) with configurable durability.
- `interaction-engine`: Provides a declarative `InteractionDefinition` registry loading from `content/interactions/` to route player clicks, sneak status, and hand tools into customizable gameplay actions.
- `rule-engine`: Evaluates Pratt AST condition expressions against simulation triggers to apply configurable gameplay rules, consequences, overcooking burn penalties, and player contact hazards from `content/rules/`.

### Modified Capabilities
- `crafting-stations`: Enhanced to support `ThermalComponent` attachment, cooking cookware stations (`cooking_pot`, `skillet`), and synchronized `ItemDisplay`, particle, and sound feedback during cooking lifecycles.

## Impact

- **API (`croppy-api`)**: New records and interfaces for `ThermalComponent`, `ThermalZone`, `InteractionDefinition`, `InteractionAction`, `RuleDefinition`, `RuleTrigger`, and `RuleAction`.
- **Core (`croppy-core`)**: New thermal state tracking, interaction router, rule evaluator leveraging `ExpressionCompiler`/`Pratt AST`, YAML loaders (`YamlInteractionLoader`, `YamlRuleLoader`), and default registry implementations.
- **Platform (`croppy-paper`)**: Integration with `PaperStationTicker` for thermal updates, particle/sound loops, display entity synchronization for pots/skillets, `StationInteractListener` delegation to `InteractionEngine`, and thermometer item handling.
- **Content Assets**: New default YAML configs for `cooking_pot.yml`, `skillet.yml`, `thermometer.yml`, `burnt_food.yml`, sample interactions, and overcooking/thermal burn rules.
