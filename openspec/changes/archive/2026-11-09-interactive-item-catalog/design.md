# Design: Interactive Item Catalog

## Context

Croppy maintains declarative custom item configurations across multiple functional categories (`seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`) and supports runtime item injection from addons. Currently, administrators distribute and inspect these items exclusively via text CLI commands (`/croppy item give` and `/croppy item list`).

This design specifies the implementation of `/croppy items`: an administrative, interactive inventory GUI spawner built atop `menu-system`, `ItemRegistry`, and `ItemFactory`. It adopts architectural patterns established by the culinary compendium (`CompendiumMenuController`), generalizing category management into a reusable `ItemCategoryRegistry`.

## Goals / Non-Goals

**Goals:**
- Provide a responsive 54-slot inventory GUI (`items_menu.yml`) for visual item browsing and administrative spawning.
- Introduce `ItemCategory` record and `ItemCategoryRegistry` port in `croppy-api`, implemented in `croppy-core` and exposed via `AddonContext`.
- Support multi-action click dispatching:
  - Left-Click: 1x to inventory.
  - Right-Click: 64x (or item max stack size) to inventory.
  - Shift-Click: Interactive suggest-command component sent to player chat.
  - Drop [Q]: Drop item entity naturally at player's feet.
- Implement dynamic horizontal pagination for category tabs (supporting arbitrary numbers of base and addon categories).
- Implement real-time item search via `ChatPromptService`.
- Dynamically augment item lore with localized shortcut hints without mutating stored `ItemDefinition` records.

**Non-Goals:**
- Replacing or modifying existing `/croppy item give` or `/croppy item list` CLI commands (retained for script and console use).
- In-game modification or creation of item YAML definitions.
- Public survival recipe book functionality (already handled by `/croppy recipes`).

## Decisions

### 1. Item Category Domain Port in `croppy-api`

We model `ItemCategory` as an immutable record in `croppy-api`:

```java
public record ItemCategory(
    Key key,
    String iconMaterial,
    Key iconItemModel,
    String displayName,
    int priority
) implements Comparable<ItemCategory> { ... }
```

`ItemCategoryRegistry` is defined as an interface port in `croppy-api` and implemented by `DefaultItemCategoryRegistry` in `croppy-core`.
Default categories are registered at startup:
- `all` (All Items, priority 0, icon: `minecraft:compass`)
- `seeds` (Seeds, priority 10, icon: `minecraft:wheat_seeds`)
- `fertilizers` (Fertilizers, priority 20, icon: `minecraft:bone_meal`)
- `watering_cans` (Watering Cans, priority 30, icon: `croppy:copper_can`)
- `tools` (Tools, priority 40, icon: `minecraft:shears`)
- `ingredients` (Ingredients, priority 50, icon: `minecraft:apple`)
- `stations` (Stations, priority 60, icon: `minecraft:crafting_table`)
- `misc` (Miscellaneous, priority 70, icon: `minecraft:bundle`)

*Alternative considered*: Hardcoding categories as an enum in `croppy-paper`. Rejected because it prevents addons from declaring new custom categories (e.g., coffee, dairy, livestock).

### 2. Session Management & Controller Architecture

`ItemCatalogController` in `croppy-paper` manages player catalog browsing state using an immutable record:

```java
public record ItemCatalogSession(
    int page,
    Key categoryKey,
    String searchQuery,
    int categoryPage
) { ... }
```

Sessions are stored in a `ConcurrentHashMap<UUID, ItemCatalogSession>` and cleared on player quit or session reset.

### 3. Layout and Matrix Specification (`items_menu.yml`)

The catalog GUI uses a 6-row (54 slots) matrix:
- **Row 0 (Slots 0..8)**: Category navigation:
  - Slot 0: Category Page Previous (`«`)
  - Slots 1..7: Dynamic Category Tabs (active tab displays enchantment glint)
  - Slot 8: Category Page Next (`»`)
- **Rows 1..4 (Slots 9..44)**: Item Display Matrix:
  - 4 rows x 7 columns = 28 item slots (`I`), framed by decorative panes (`X`)
- **Row 5 (Slots 45..53)**: Action Controls:
  - Slot 45: Item Page Previous (`<`)
  - Slot 47: Search button (`Q` - triggers `ChatPromptService`)
  - Slot 49: Info / Overview (`?` - displays category, page, and total count)
  - Slot 51: Clear Filters (`C` - resets category to `all` and query to null)
  - Slot 53: Item Page Next (`>`)

### 4. Click Dispatching and Anti-Duplication via `PaperMenuListener`

All item cards in the catalog are tagged with a PDC string key `croppy:catalog_item_id`.
In `PaperMenuListener.onInventoryClick`:
1. When clicked inside an `items_menu`, the event is unconditionally cancelled to prevent item theft or placement.
2. If the clicked slot holds `croppy:catalog_item_id`:
   - `LEFT`: Generate 1x `ItemStack` via `itemFactory.createItemStack(def, 1)`. Add to inventory; drop remaining at player feet if full. Play `Sound.ENTITY_ITEM_PICKUP`.
   - `RIGHT`: Generate `def.material().getMaxStackSize()` units. Add to inventory; drop remaining. Play `Sound.ENTITY_ITEM_PICKUP`.
   - `SHIFT_LEFT` / `SHIFT_RIGHT`: Send MiniMessage component to chat:
     `<gray>Command: <yellow><click:suggest_command:'/croppy item give <category> <id> 1'>/croppy item give <category> <id> 1</click></yellow></gray>`.
   - `DROP` / `CONTROL_DROP`: Spawn item naturally at player's feet via `world.dropItemNaturally()`.

### 5. Dynamic Lore Augmentation

When rendering an item stack for a catalog slot, `ItemCatalogController` copies the `ItemStack` from `itemFactory.createItemStack(def, 1)`, and appends localized administrative action instructions to its lore:
```text
<dark_gray>------------------------</dark_gray>
<yellow>Left-Click:</yellow> <gray>Give 1x</gray>
<yellow>Right-Click:</yellow> <gray>Give 64x (Stack)</gray>
<yellow>Shift-Click:</yellow> <gray>Suggest /give command</gray>
<yellow>Drop [Q]:</yellow> <gray>Drop at feet</gray>
```
The original `ItemDefinition` record remains untouched and thread-safe.

## Risks / Trade-offs

- **[Risk] High item count causing UI lag during search or pagination**  
  → *Mitigation*: Items are filtered and paginated in-memory over `ItemRegistry.values()` (typically a few hundred items max), which takes < 1ms. Calculations run synchronously on the main thread during navigation.
- **[Risk] Inventory overflow when clicking full stack**  
  → *Mitigation*: Bukkit's `addItem()` returns unplaced items in a Map; any remainder is immediately dropped at the player's feet with `world.dropItemNaturally()`, preventing loss.
- **[Risk] Addon unloads with custom categories in active sessions**  
  → *Mitigation*: `ItemCategoryRegistry` supports unregistering categories. If a viewer has an unregistered category selected, `ItemCatalogController` falls back safely to `all`.
