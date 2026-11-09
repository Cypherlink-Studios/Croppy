# Proposal: Fix Coffee Station Display Models

## Why

When players place artisan crafting stations from the `croppy-coffee` expansion addon (`coffee_grinder`, `coffee_roaster`, and `espresso_machine`) in the world, the stations fail to render their custom 3D Blockbench models. Instead, an `ItemDisplay` rendering a flat sheet of paper (`minecraft:paper`) is projected, accompanied by a floating status text display over an invisible barrier block.

This occurs because:
1. Station YAML definitions declare `base_model.item` keys (e.g. `croppy_coffee:block/coffee_grinder`) that do not exist in the domain `ItemRegistry`, causing `StationViewerTracker.resolveItemStack` to fall back to `new ItemStack(Material.PAPER)`.
2. Station item YAML configurations specify `item-model` keys pointing to `item/` instead of `block/`, conflicting with the resource pack's directory structure (`assets/croppy_coffee/items/block/*.json`).
3. Typographical errors between `expresso` and `espresso` across file names and model targets in `assets/` and station YAMLs break model resolution for the espresso machine.

Fixing these identifier links, path references, and typos ensures all three coffee stations project their intended 3D models seamlessly in Minecraft 1.21.8.

## What Changes

- **Update Station Base Model Item References**:
  - In `addons/coffee/src/main/resources/stations/coffee_grinder.yml`, set `base_model.item` to `"croppy_coffee:coffee_grinder"`.
  - In `addons/coffee/src/main/resources/stations/coffee_roaster.yml`, set `base_model.item` to `"croppy_coffee:coffee_roaster"`.
  - In `addons/coffee/src/main/resources/stations/espresso_machine.yml`, set `base_model.item` to `"croppy_coffee:espresso_machine"`.

- **Align Station Item Model Keys**:
  - In `addons/coffee/src/main/resources/items/stations/coffee_grinder.yml`, update `item-model` to `"croppy_coffee:block/coffee_grinder"`.
  - In `addons/coffee/src/main/resources/items/stations/coffee_roaster.yml`, update `item-model` to `"croppy_coffee:block/coffee_roaster"`.
  - In `addons/coffee/src/main/resources/items/stations/espresso_machine.yml`, update `item-model` to `"croppy_coffee:block/espresso_machine"`.

- **Resolve Espresso Machine Typos in Resource Assets**:
  - Rename `addons/coffee/src/main/resources/assets/croppy_coffee/items/block/expresso_machine.json` to `espresso_machine.json`.
  - Update model reference inside `espresso_machine.json` from `"croppy_coffee:block/expresso_machine"` to `"croppy_coffee:block/espresso_machine"`.

- **Align Seed and Ingredient Item Model Paths**:
  - Correct `coffee_sapling.yml`, `coffee_seeds.yml`, and `green_coffee_beans.yml` to specify `croppy_coffee:item/<id>` matching their paths in `assets/croppy_coffee/items/item/`.

- **Automated Integrity Tests**:
  - Add test coverage in `CoffeeResourcesIntegrityTest` to assert that all station `base_model.item` identifiers exist in the item registry and all declared `item-model` components match actual resource pack JSON definitions.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `station-virtual-displays`: Clarify and require that station visual base models resolve against registered `ItemDefinition` keys so modern `item_model` data components and transforms propagate to client display entities.

## Impact

- **Affected Code & Addon Files**:
  - `addons/coffee/src/main/resources/stations/coffee_grinder.yml`
  - `addons/coffee/src/main/resources/stations/coffee_roaster.yml`
  - `addons/coffee/src/main/resources/stations/espresso_machine.yml`
  - `addons/coffee/src/main/resources/items/stations/coffee_grinder.yml`
  - `addons/coffee/src/main/resources/items/stations/coffee_roaster.yml`
  - `addons/coffee/src/main/resources/items/stations/espresso_machine.yml`
  - `addons/coffee/src/main/resources/items/seeds/coffee_sapling.yml`
  - `addons/coffee/src/main/resources/items/seeds/coffee_seeds.yml`
  - `addons/coffee/src/main/resources/items/ingredients/green_coffee_beans.yml`
  - `addons/coffee/src/main/resources/assets/croppy_coffee/items/block/espresso_machine.json`
  - `addons/coffee/src/test/java/dev/darkblade/croppy/coffee/CoffeeResourcesIntegrityTest.java`
- **APIs & Dependencies**: Zero breaking API changes in `croppy-api`, `croppy-core`, or `croppy-paper`. No external dependencies modified.
- **Client Visuals**: Fixes floating paper glitch when placing coffee stations; correctly renders full 3D Blockbench models in-game.
