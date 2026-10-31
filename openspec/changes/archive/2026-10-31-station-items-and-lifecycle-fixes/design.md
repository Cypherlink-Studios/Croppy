# Design

## Context

Croppy uses a centralized `ItemRegistry` where items are loaded from categorized YAML files under `plugins/Croppy/items/<category>/<item_id>.yml` and tagged with PDC `croppy:item_id` and `croppy:category`. Crafting stations (`croppy:cooking_pot` and `croppy:skillet`) were configured in `stations/`, but had no corresponding `ItemDefinition` in `items/stations/`. Consequently, players could not obtain them via `/croppy item give stations <id>`, and placing vanilla items did not trigger `StationBlockListener`'s PDC check. Additionally, `ChunkLifecycleListener` handled only crops, leaving stations vulnerable to memory loss when chunks unloaded, and physical station interactions were completely silent when clicked without ingredients.

## Goals / Non-Goals

**Goals:**
- Provide default YAML item definitions under `items/stations/` (`cooking_pot.yml` and `skillet.yml`) with MiniMessage formatting, materials, and lore.
- Extract and register station item definitions during plugin startup (`setupItemDefinitions()`).
- Drop the station custom item upon block destruction in `StationBlockListener.onBlockBreak` along with its stored inventory contents.
- Coordinate station chunk persistence and memory eviction in `ChunkLifecycleListener` alongside crop persistence.
- Provide responsive MiniMessage action bar feedback in `StationInteractListener` when interacting with an empty physical station.

**Non-Goals:**
- Creating dedicated shortcut commands (e.g., `/croppy station give`); standard `/croppy item give stations <item_id>` is utilized.
- Altering core domain interfaces or binary serialization codecs in `croppy-api` and `croppy-core`.

## Decisions

### 1. Dedicated YAML Items in `items/stations/` vs. Synthetic Items
* **Chosen**: Dedicated YAML files (`items/stations/cooking_pot.yml` and `items/stations/skillet.yml`).
* **Rationale**: Maintains consistency with `seeds`, `fertilizers`, `watering_cans`, and `tools`. Server administrators can customize material, custom model data, name, and lore without modifying station mechanics.
* **Alternative Considered**: Dynamically synthesizing `ItemDefinition` instances in memory from `StationDefinition`. Rejected because it prevents server owners from adjusting item visuals/lore independently from machine simulation properties.

### 2. Unified `ChunkLifecycleListener` for Crops & Stations
* **Chosen**: Extend `ChunkLifecycleListener` to accept `StationStorage` and `DefaultStationManager` (or nullable/optional references for modularity).
* **Rationale**: Bukkit `ChunkLoadEvent` and `ChunkUnloadEvent` already manage spatial chunk life for crops. Handling stations in the same listener ensures synchronized persistence, avoids duplicate event registration overhead, and prevents orphaned stations in unloaded chunks.
* **Alternative Considered**: Creating a separate `StationChunkListener`. Rejected to avoid redundant chunk event registrations.

### 3. Dropping Station Item via `ItemFactory`
* **Chosen**: When `StationBlockListener.onBlockBreak` runs, retrieve the station's `ItemDefinition` from `itemRegistry.get(station.getStationKey())` and drop `itemFactory.createItemStack(def, 1)`.
* **Rationale**: Ensures the dropped block returns with the exact same PDC metadata (`croppy:item_id`, `croppy:category`) needed to place it again elsewhere. If no custom item is found, fall back to the base material from `station.getDefinition().baseBlock()`.

### 4. Non-Intrusive Action Bar Feedback for Empty Physical Stations
* **Chosen**: Send an action bar notification (e.g., `<gradient:#ffaa00:#ff3300>Skillet</gradient> <gray>[Empty - %.1f°C]</gray>`) and subtle audio feedback (`BLOCK_DISPENSER_FAIL` or `ITEM_BUNDLE_DROP_CONTENTS`) when a player right-clicks an empty physical station.
* **Rationale**: Prevents confusion without cluttering the chat log.

## Risks / Trade-offs

* **[Risk]** Breaking a station might duplicate or delete items if the player is in creative mode or if another plugin cancels the event.
  * **Mitigation**: `StationBlockListener.onBlockBreak` listens on `EventPriority.MONITOR, ignoreCancelled = true`. The station item and stored items are only dropped if the station was actively present and successfully removed from `DefaultStationManager`.
* **[Risk]** Chunk unloading performance under heavy chunk churn.
  * **Mitigation**: Station state is serialized via `StationBinaryCodec` into chunk PDC, which is fast, thread-safe, and only executes for chunks that actually contain stations.
