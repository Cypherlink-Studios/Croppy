# Design: Addon Core Prerequisites (M0)

## 1. Shared DatabaseService with Table-Prefix Isolation
- `DatabaseService` (in `dev.darkblade.croppy.api.storage`, extends `Service`) provides:
  - `Connection getConnection() throws SQLException`
  - `String getTablePrefix()`
  - `String table(String name)` — validates `[a-zA-Z0-9_]+` and returns `prefix + name`
  - `DatabaseService scoped(String prefix)` — returns a view sharing the same `DataSource` with a distinct prefix
  - `execute(...)` and `query(...)` functional helpers
- `PaperAddonContext.getDatabaseService()` returns `rootDatabaseService.scoped("addon_" + sanitize(addonId) + "_")`.
- `SqliteDatabaseService` in `croppy-paper` manages the HikariCP pool (sharing `storage.sqlite.file` or `croppy.db` in `plugin.getDataFolder()`), ensuring `LibbyDependencyLoader.loadDatabaseDrivers()` runs before pool creation.

## 2. ProcessEngine Lifecycle Hooks
- `ProcessListener` in `dev.darkblade.croppy.api.process` defines default no-op methods:
  - `onProcessStart(StationInstance, ProcessDefinition, ProcessInstance)`
  - `onProcessTick(StationInstance, ProcessDefinition, ProcessInstance, long deltaTicks)`
  - `onPhaseAdvance(StationInstance, ProcessDefinition, ProcessInstance, int prevIndex, int newIndex)`
  - `onProcessComplete(StationInstance, ProcessDefinition, ProcessInstance, List<StoredItem> outputs)`
  - `onProcessFail(StationInstance, ProcessDefinition, ProcessInstance, PhaseDefinition failedPhase)`
- `ProcessEngine` maintains a `CopyOnWriteArrayList<ProcessListener>` and wraps every callback in `try-catch` so listener errors never halt station simulation.

## 3. StoredItem Metadata & Quality Round-Trip
- `ItemMetadataContributor` (`Keyed`) in `dev.darkblade.croppy.api.item`:
  - `Map<String, String> serializeMetadata(StoredItem item)` — returns namespaced PDC string entries
  - `List<String> formatLore(StoredItem item)` — returns optional MiniMessage lore lines
  - `Map<String, Object> deserializeMetadata(Map<String, String> pdcTags)` — extracts domain properties from PDC
- `ItemFactory` writes `croppy:quality` (when `quality > 0`) and contributor PDC tags/lore in `createBukkitItem(StoredItem)`, and provides `Optional<StoredItem> toStoredItem(ItemStack)` which `StationGuiListener` uses when syncing GUI slots back to `StorageComponent`.

## 4. StationBinaryCodec Backward-Compatible Extension
- Bump `StationBinaryCodec` magic/version or append optional flags safely:
  - Read v1/v2 streams without failure.
  - Write v3 with `StoredItem.properties()` (UTF key + typed primitive/String value) and `ProcessInstance.contextVariables` (simple String/Number/Boolean entries).
