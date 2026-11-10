# Spec Delta

## Purpose
Provides a declarative Bukkit crafting recipe system for Croppy, allowing custom stations, tools, and items to be crafted at standard Minecraft crafting tables via shaped and shapeless recipes.

## ADDED Requirements

### Requirement: Declarative Crafting Recipe Model
The system SHALL represent crafting recipes using immutable domain records identifying unique namespaced keys, crafting type (shaped or shapeless), optional recipe book group and category, recipe shape pattern, ingredient mappings, and resulting item stack specifications.

#### Scenario: Parse shaped crafting recipe
- **WHEN** a recipe YAML specifies `type: "shaped"`, a 1x1 to 3x3 `shape` matrix, `ingredients` character map, and `result`
- **THEN** the system parses the recipe into an immutable `CraftingRecipe` with type `SHAPED` and validated ingredient mappings

#### Scenario: Parse shapeless crafting recipe
- **WHEN** a recipe YAML specifies `type: "shapeless"`, an unordered list of `ingredients` with amounts, and `result`
- **THEN** the system parses the recipe into an immutable `CraftingRecipe` with type `SHAPELESS`

#### Scenario: Reject recipe with mismatched shape and ingredient symbols
- **WHEN** a shaped recipe declares a character in `shape` that is missing from the `ingredients` mapping
- **THEN** the system logs a validation error and rejects the recipe from registration

### Requirement: Ingredient Choice Resolution
The system SHALL resolve ingredient declarations into platform recipe choices, mapping vanilla materials to material choices, vanilla tags to tag-backed material choices, and custom Croppy item keys to exact item stack choices enforcing persistent data container tags and metadata.

#### Scenario: Resolve vanilla material ingredient
- **WHEN** an ingredient declaration specifies a vanilla material identifier (`minecraft:iron_ingot`)
- **THEN** the system resolves the ingredient to a Bukkit `RecipeChoice.MaterialChoice` accepting that material

#### Scenario: Resolve vanilla tag ingredient
- **WHEN** an ingredient declaration specifies a tag reference (`#minecraft:planks`)
- **THEN** the system resolves the ingredient to a Bukkit `RecipeChoice.MaterialChoice` matching all materials in the vanilla tag

#### Scenario: Resolve custom Croppy item ingredient
- **WHEN** an ingredient declaration specifies a custom item key (`croppy:wooden_can`)
- **THEN** the system resolves the ingredient to a Bukkit `RecipeChoice.ExactChoice` matching the custom item's PersistentDataContainer tags, model, and display name

#### Scenario: Reject non-existent custom item ingredient
- **WHEN** an ingredient references a `croppy:*` key not present in `ItemRegistry`
- **THEN** the system logs a descriptive error and skips registration of the dependent recipe

### Requirement: Bukkit Recipe Registration and Lifecycle Management
The system SHALL register valid shaped and shapeless crafting recipes with the Bukkit server recipe manager upon startup or reload, track all registered recipe keys, and cleanly unregister them during plugin disable or configuration reload.

#### Scenario: Register shaped recipe with Bukkit
- **WHEN** a valid shaped recipe definition is registered by the recipe service
- **THEN** a corresponding `org.bukkit.inventory.ShapedRecipe` is added to Bukkit's recipe manager under its `NamespacedKey`

#### Scenario: Register shapeless recipe with Bukkit
- **WHEN** a valid shapeless recipe definition is registered by the recipe service
- **THEN** a corresponding `org.bukkit.inventory.ShapelessRecipe` is added to Bukkit's recipe manager under its `NamespacedKey`

#### Scenario: Clean unregister on reload or disable
- **WHEN** the plugin is disabled or a configuration reload is invoked
- **THEN** the system unregisters all tracked Croppy recipe keys from Bukkit's recipe manager without leaving orphaned recipes

### Requirement: Recipe Book Auto-Discovery
The system SHALL provide configurable automatic recipe discovery for players, discovering all registered Croppy crafting recipe keys in a player's recipe book upon server join when enabled in configuration.

#### Scenario: Auto-discover recipes on player join
- **WHEN** a player joins the server and `crafting.auto-discover-recipes` is set to `true`
- **THEN** the system invokes recipe discovery for all registered Croppy crafting recipe keys on the joining player

#### Scenario: Respect disabled auto-discovery
- **WHEN** a player joins the server and `crafting.auto-discover-recipes` is set to `false`
- **THEN** the system does not automatically grant Croppy recipe keys to the joining player

### Requirement: Bundled Starter Crafting Recipes
The system SHALL provide bundled default crafting recipe definitions for core interactive stations, agricultural tools, and tiered watering cans, extracting missing default recipe files on startup.

#### Scenario: Extract default station recipes
- **WHEN** the plugin initializes with missing crafting recipe files
- **THEN** default recipe files for `croppy:cooking_pot`, `croppy:stove`, and `croppy:skillet` are extracted to `plugins/Croppy/crafting/stations/`

#### Scenario: Craft cooking pot at crafting table
- **WHEN** a player arranges 4 iron ingots and 1 cauldron matching `croppy:cooking_pot` recipe
- **THEN** the crafting output displays 1 custom `croppy:cooking_pot` item with its custom model and PDC tags

#### Scenario: Upgrade watering can tier at crafting table
- **WHEN** a player crafts `croppy:copper_can` using a `croppy:wooden_can` and copper ingots
- **THEN** the crafting output yields a `croppy:copper_can` only when the input watering can has valid Croppy PDC metadata
