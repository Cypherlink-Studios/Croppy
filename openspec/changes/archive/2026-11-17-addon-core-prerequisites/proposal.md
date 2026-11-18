# Proposal: Addon Core Prerequisites (M0)

## Why
First-party and third-party Croppy addons (starting with **Croppy Brewery**) need clean, platform-independent extension points in `croppy-api`, `croppy-core`, and `croppy-paper` to:
1. Share a single SQLite database pool with isolated, table-prefixed schemas per addon.
2. React to `ProcessEngine` lifecycle events (`start`, `tick`, `phaseAdvance`, `complete`, `fail`) without duplicating station ticking.
3. Preserve `StoredItem` quality and custom properties across `ItemFactory` (`ItemStack` ↔ `StoredItem`) and `StationBinaryCodec` persistence.
4. Access `DataTypeRegistry`, `RecipeDiscoveryEngine`, `DatabaseService`, and `ProcessListener` registration via `AddonContext`.

## What Changes
- **`croppy-api`**:
  - Add `DatabaseService` port with table-prefix scoping (`scoped(String prefix)`, `table(String name)`).
  - Add `ProcessListener` contract for `ProcessEngine` lifecycle callbacks.
  - Add `ItemMetadataContributor` / `ItemMetadataRegistry` (pure Java) for `StoredItem` ↔ PDC/lore metadata round-trips.
  - Extend `AddonContext` with `getDataTypeRegistry()`, `getRecipeDiscoveryEngine()`, `getDatabaseService()`, `getItemMetadataRegistry()`, and `registerProcessListener(ProcessListener)`.
- **`croppy-core`**:
  - Add `ProcessListener` dispatch to `ProcessEngine`.
  - Update `StationBinaryCodec` to persist `StoredItem.properties()` and `ProcessInstance` string/numeric context variables with backward compatibility.
- **`croppy-paper`**:
  - Implement `SqliteDatabaseService` (HikariCP + SQLite) and wire it into `CroppyPlugin` and `PaperAddonContext`.
  - Update `ItemFactory` (`createBukkitItem` / `toStoredItem`) and `StationGuiListener` to preserve `StoredItem.quality()` and `StoredItem.properties()` via PDC and registered `ItemMetadataContributor`s.
  - Track and clean up addon-registered `ProcessListener`s and `ItemMetadataContributor`s on addon disable.

## Impact
- Additive, backward-compatible changes to `croppy-api`, `croppy-core`, and `croppy-paper`.
- Zero Bukkit dependencies added to `croppy-api` or `croppy-core`.
