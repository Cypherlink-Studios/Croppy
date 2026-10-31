# Proposal

## Why

In-game testing revealed that placing station blocks using vanilla items failed to register them as active stations, leaving player interactions completely unresponsive and silent. Stations lacked dedicated custom item definitions in `items/stations/`, block destruction failed to drop the station item itself, dynamic chunk loading/unloading omitted station persistence synchronization, and physical station interactions provided no feedback when empty.

## What Changes

- **Station Custom Item Definitions**: Add dedicated YAML definitions for cookware stations (`items/stations/cooking_pot.yml` and `items/stations/skillet.yml`) within the `stations` category, bundled as default plugin resources and loaded into `ItemRegistry`.
- **Station Item Drop on Destruction**: Update `StationBlockListener.onBlockBreak` so that breaking a station drops the station's custom item into the world alongside its stored input/output contents.
- **Dynamic Chunk Lifecycle Persistence**: Integrate `StationStorage` with `ChunkLifecycleListener` so that station instances are loaded from disk when chunks load and saved/evicted when chunks unload.
- **Physical Empty Station Feedback**: Enhance `StationInteractListener` to deliver MiniMessage action bar feedback when players interact with an empty physical station (such as a skillet with no active process or items), preventing unresponsive silence.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `crafting-stations`: Update requirements and scenarios for block destruction drops (dropping the station item), chunk lifecycle persistence synchronization on load/unload, and player feedback during empty physical interactions.

## Impact

- **Affected Files**:
  - `croppy-paper/src/main/resources/items/stations/cooking_pot.yml` (new resource)
  - `croppy-paper/src/main/resources/items/stations/skillet.yml` (new resource)
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/CroppyPlugin.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/station/listener/StationBlockListener.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/station/listener/StationInteractListener.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/listener/ChunkLifecycleListener.java`
- **APIs & Dependencies**: No breaking API changes in `croppy-api` or `croppy-core`. Fully backwards-compatible enhancement in `croppy-paper` and default YAML configurations.
