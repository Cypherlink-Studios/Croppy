# Design

## Context

See `proposal.md` for background and motivation.

Croppy currently models species in `species/` where crop block lifecycle and seed item configuration are coupled. Single-purpose administrative commands (`/croppy give`, `/croppy givecan`, `/croppy givefertilizer`, `/croppy tool`) were created ad-hoc, with some lacking Cloud 2.0 suggestion providers. Furthermore, different custom items (watering cans, fertilizers, tools) each used disparate PDC tag keys (`croppy:tool`, `croppy:watering_can_tier`, `croppy:fertilizer_id`).

To ensure scalability and clean hexagonal boundaries, all custom items must be defined via a unified `ItemDefinition`, registered in an `ItemRegistry`, loaded from `items/<category>/*.yml`, stamped with standard PDC tags, and managed through unified commands.

## Goals / Non-Goals

**Goals:**
- Provide a pure Java 21 `ItemDefinition` record and `ItemRegistry` in `croppy-api` / `croppy-core` with zero Bukkit dependencies.
- Support YAML item definitions across multiple categories (`seeds`, `fertilizers`, `watering_cans`, `tools`) under `items/<category>/<item_id>.yml`.
- Standardize Bukkit `PersistentDataContainer` tagging on all generated items with `croppy:item_id` and `croppy:category`, plus arbitrary custom PDC key-value properties.
- Rename `species/` directory to `crops/`, restricting its scope to block lifecycle, requirements, stages, and harvest drops.
- Implement `/croppy item give <category> <item_id> [quantity]` with `--target <player>` and `--silent` flags.
- Implement dynamic Incendo Cloud 2.0 suggestions: `@Suggestions("item_categories")` and contextual `@Suggestions("category_items")`.
- Provide backward-compatible item identification in listeners for legacy PDC tags.

**Non-Goals:**
- In-game custom item editing or crafting recipe GUI editors.
- Dynamic runtime creation of new item categories beyond filesystem subdirectories.
- Modifying underlying crop block persistence schemas in PDC or SQLite.

## Decisions

### 1. Hexagonal Item Modeling (`ItemDefinition` & `ItemRegistry`)

`croppy-api` will declare:
```
ItemDefinition(
    Key key,
    String category,
    String materialKey,
    int customModelData,
    String displayName,
    List<String> lore,
    Map<String, String> pdcProperties
)
```
And `ItemRegistry`:
```
public interface ItemRegistry {
    void register(ItemDefinition definition);
    Optional<ItemDefinition> get(Key key);
    Collection<ItemDefinition> getAll();
    Collection<ItemDefinition> getByCategory(String category);
    Set<String> getCategories();
    void clear();
}
```
`croppy-core` implements `DefaultItemRegistry` using `ConcurrentHashMap` for lock-free reads, and `YamlItemLoader` using SnakeYAML.

*Alternatives Considered:*
- *Define item logic directly in croppy-paper*: Rejected because domain systems (recipes, drop tables, future cooking) need to reference item definitions without depending on the Paper platform runtime.

### 2. Standardized PDC Structure

All items instantiated via `ItemFactory` in `croppy-paper` write:
- `croppy:item_id` (PersistentDataType.STRING): full namespaced key string (e.g., `croppy:speed_gro`).
- `croppy:category` (PersistentDataType.STRING): category name (e.g., `fertilizers`).
- Dynamic runtime state:
  - Watering cans: `croppy:water_uses` (INTEGER).
- Arbitrary YAML PDC properties:
  - Loaded from `pdc:` mapping in definition YAML, written as String types under `new NamespacedKey("croppy", subKey)` or parsed `NamespacedKey`.

*Compatibility Fallback:*
Listeners (`WateringListener`, `FertilizerListener`, `InspectionToolListener`) will check `croppy:item_id` first; if absent, they check legacy keys (`croppy:watering_can_tier`, `croppy:fertilizer_id`, `croppy:tool`) to preserve compatibility with existing player inventory items.

### 3. Contextual Suggestions in Cloud 2.0

Cloud Paper 2.0 allows contextual suggestion providers:
```java
@SuggestionProvider("item_categories")
public List<String> suggestCategories(CommandContext<CommandSourceStack> ctx, CommandInput input) {
    return List.copyOf(itemRegistry.getCategories());
}

@SuggestionProvider("category_items")
public List<String> suggestCategoryItems(CommandContext<CommandSourceStack> ctx, CommandInput input) {
    String category = ctx.getOrDefault("category", null);
    if (category == null) {
        return itemRegistry.getAll().stream().map(d -> d.key().asString()).toList();
    }
    return itemRegistry.getByCategory(category).stream().map(d -> d.key().asString()).toList();
}
```
This guarantees that tab completion on `<item_id>` automatically filters suggestions based on the chosen `<category>`.

### 4. Separation of Crop Lifecycle and Seed Item

Crop lifecycle YAMLs in `crops/<crop_id>.yml` will reference the seed item definition by its namespaced Key (e.g., `seed: "croppy:wheat_seeds"`). The seed item definition in `items/seeds/wheat_seeds.yml` will define the physical item (`WHEAT_SEEDS`, custom model data, name, lore).
`CropPlantListener` will identify planting attempts by querying the item in player hand via `croppy:item_id`, locating the corresponding crop species that requires that seed Key.

### Architectural Diagram

```
+-------------------------------------------------------+
|                    croppy-api                         |
|  - ItemDefinition (Record)                            |
|  - ItemRegistry (Interface Port)                      |
|  - Key / Species / CropInstance                       |
+-------------------------------------------------------+
                           ^
                           | implements / uses
+-------------------------------------------------------+
|                    croppy-core                        |
|  - DefaultItemRegistry (ConcurrentHashMap)           |
|  - YamlItemLoader (items/<category>/*.yml)            |
|  - YamlSpeciesLoader (crops/*.yml)                    |
+-------------------------------------------------------+
                           ^
                           | adapts / registers
+-------------------------------------------------------+
|                    croppy-paper                       |
|  - ItemFactory / PaperItemService                     |
|  - CroppyCommands (item give/list + suggestions)      |
|  - Listeners (Watering, Fertilizer, Plant, Inspect)   |
|  - Standardized PDC (croppy:item_id, category)        |
+-------------------------------------------------------+
```

## Risks / Trade-offs

- **[Risk] Existing items in player inventories lose functionality after refactor**
  - *Mitigation*: Maintain fallback checks in all listeners for legacy PDC keys (`croppy:watering_can_tier`, `croppy:fertilizer_id`, `croppy:tool`).
- **[Risk] Breaking existing scripts/macros using legacy commands (`/croppy givecan`, `/croppy givefertilizer`)**
  - *Mitigation*: Clearly document breaking change in proposal and migration notes. Command feedback displays informative usage when invalid commands are attempted.
- **[Risk] Invalid Bukkit Material strings in YAML**
  - *Mitigation*: `ItemFactory` safely validates materials with `Material.matchMaterial()`, logging a clear error and substituting a placeholder material (`BARRIER`) instead of throwing an unhandled exception.

## Migration Plan

1. Rename resource folder `species/` to `crops/`.
2. Move seed item attributes out of `crops/wheat.yml` into `items/seeds/wheat_seeds.yml`.
3. Move fertilizer definitions from `fertilizers/` to `items/fertilizers/`.
4. Move watering can configurations to `items/watering_cans/`.
5. Create inspection tool configuration under `items/tools/inspector.yml`.
6. Update plugin reload and initialization routines to load `items/` tree first, then `crops/`.
