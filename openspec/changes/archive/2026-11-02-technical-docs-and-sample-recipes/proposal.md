# Proposal

## Why

While Croppy possesses robust domain engines and YAML loaders for recipes, crafting stations, failure rules, and interactions, these configuration systems lack technical YAML documentation and schema references in both English and Spanish documentation catalogs (`docs/en/` and `docs/es/`). Furthermore, the server distribution lacks default out-of-the-box recipe YAML definitions, leaving the culinary compendium and cooking stations empty upon initial deployment.

## What Changes

- **Technical Documentation Pages (Option A - Modular)**:
  - Create dedicated `recipes-configuration.md` in `docs/en/` and `docs/es/` documenting the complete YAML schema of `YamlRecipeLoader` (ingredients, station bindings, process IDs, discovery methods, hints, and secrets).
  - Create dedicated `workstations-configuration.md` in `docs/en/` and `docs/es/` documenting physical station definitions, capacities, PacketEvents 3D virtual displays (`process_display` and `text_display`), and thermal properties.
  - Create dedicated `rules-and-interactions.md` in `docs/en/` and `docs/es/` documenting station interaction triggers (`interactions/*.yml`) and dynamic cooking rules with Pratt AST condition grammar (`rules/*.yml`).
  - Update `items-and-tools.md` in `docs/en/` and `docs/es/` with full YAML schema reference for custom items, Minecraft 1.21.4+ `item_model` data components, and PDC tags.
  - Update `soil-and-fertilizers.md` in `docs/en/` and `docs/es/` with complete YAML schema reference for soil fertilizers (`SPEED`, `QUALITY`, `RETAINING`).
  - Re-index sidebar navigation ordering (1 to 18) across both language trees for seamless browsing in Starlight.
- **Bundled Default Recipes & Food Items**:
  - Author default sample recipes in `croppy-paper/src/main/resources/recipes/`:
    - `tomato_soup.yml`: Simmered soup prepared in `croppy:cooking_pot`.
    - `vegetable_stew.yml`: Hearty vegetable stew prepared in `croppy:cooking_pot`.
    - `fried_egg.yml`: Searing egg cooked in `croppy:skillet`.
  - Author supporting default culinary food items:
    - `croppy:tomato_soup` (`items/ingredients/tomato_soup.yml`).
    - `croppy:vegetable_stew` (`items/ingredients/vegetable_stew.yml`).
    - `croppy:fried_egg` (`items/ingredients/fried_egg.yml`).
  - Update `CroppyPlugin#setupRecipeDefinitions()` and `setupItemDefinitions()` to automatically unpack default recipes and items if absent.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `recipe-discovery`: Adds a requirement for bundled default recipe definitions and automatic unpacking into the server's `recipes/` directory on startup.

## Impact

- **Documentation**: 3 new technical documentation files in `docs/en/` and 3 in `docs/es/`, plus updates to existing item and fertilizer docs and sidebar ordering.
- **Runtime Assets**: Adds 3 recipe definitions and 3 culinary item definitions to `croppy-paper` JAR resources.
- **Code**: `CroppyPlugin.java` unpacks default recipes on first run and loads them into `RecipeRegistry`.
- **Compatibility**: 100% backward compatible; existing custom recipes and items remain unaffected.
