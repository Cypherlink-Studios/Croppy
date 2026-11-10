# Proposal

## Why

Currently, custom items in Croppy—including interactive crafting stations (`croppy:stove`, `croppy:cooking_pot`, `croppy:skillet`), tiered watering cans, and agricultural tools—can only be acquired through administrative commands or creative catalog interfaces. Survival gameplay requires a native, vanilla-compatible crafting mechanism that allows players to fabricate stations and tools at standard crafting tables using recipes composed of vanilla Minecraft materials, tags, or other custom Croppy items.

Implementing a declarative Bukkit crafting recipe system enables server administrators and addon developers to define shaped and shapeless crafting recipes in configuration files, registers them into the Bukkit recipe registry with proper `RecipeChoice` resolution (including PersistentDataContainer matching for custom items), ensures clean registration lifecycle during plugin reloads, and exposes recipe discovery in vanilla recipe books.

## What Changes

- Introduce domain models in `croppy-api` for crafting recipe definitions (`CraftingRecipe`, `CraftingType`, `IngredientChoice`, and `CraftingRecipeRegistry` port).
- Implement `DefaultCraftingRecipeRegistry` and `YamlCraftingRecipeLoader` in `croppy-core` to load shaped and shapeless recipe definitions from `plugins/Croppy/crafting/**/*.yml`.
- Implement `PaperCraftingRecipeService` in `croppy-paper` to adapt domain crafting recipes into Bukkit `ShapedRecipe` and `ShapelessRecipe` instances:
  - Map vanilla materials (`minecraft:<material>`) to `RecipeChoice.MaterialChoice`.
  - Map vanilla item tags (`#minecraft:<tag>`) to tagged `RecipeChoice.MaterialChoice`.
  - Map custom items (`croppy:<item_id>`) to `RecipeChoice.ExactChoice` using `ItemFactory` to enforce PDC and metadata integrity.
  - Produce resulting `ItemStack` through `ItemFactory`.
- Manage the Bukkit recipe lifecycle: track registered recipe keys and cleanly unregister them (`Bukkit.removeRecipe`) on plugin disable or `/croppy reload`.
- Provide configurable automatic recipe discovery for players (`crafting.auto-discover-recipes: true` in `config.yml`) via `PlayerJoinEvent`.
- Integrate crafting recipe registration into `AddonContext` and support declarative `crafting/` discovery for Croppy addons.
- Bundle default starter recipes for core stations (`stove`, `cooking_pot`, `skillet`), tools (`inspector`, `thermometer`), and tiered watering cans (`wooden_can`, `copper_can`, `iron_can`).

## Capabilities

### New Capabilities
- `crafting-recipes`: Declarative shaped and shapeless Bukkit crafting recipes, ingredient mapping (vanilla materials, tags, Croppy custom items via `ExactChoice`), server recipe registration, reload lifecycle management, and recipe book auto-discovery.

### Modified Capabilities
- `addon-system`: Expose `CraftingRecipeRegistry` on `AddonContext` and support declarative discovery of `crafting/**/*.yml` in addon packages.

## Impact

- **API (`croppy-api`)**: Adds `dev.darkblade.croppy.api.crafting` package with `CraftingRecipe`, `CraftingType`, `IngredientChoice`, and `CraftingRecipeRegistry`. Adds `getCraftingRecipeRegistry()` to `AddonContext`.
- **Core (`croppy-core`)**: Adds `DefaultCraftingRecipeRegistry` and `YamlCraftingRecipeLoader`. Adds crafting registry access to `CroppyCore`.
- **Platform (`croppy-paper`)**: Adds `PaperCraftingRecipeService`, `CraftingRecipeChoiceResolver`, and recipe discovery listener. Adds `crafting/` resource extraction in `CroppyPlugin`.
- **Configuration & Resources**: Adds default YAML files in `crafting/stations/`, `crafting/tools/`, and `crafting/watering_cans/`, and `crafting` section in `config.yml`.
- **Dependencies & Performance**: Zero new third-party dependencies. Crafting recipes are registered once at startup/reload on the server main thread and evaluated by Minecraft's native recipe lookup.
