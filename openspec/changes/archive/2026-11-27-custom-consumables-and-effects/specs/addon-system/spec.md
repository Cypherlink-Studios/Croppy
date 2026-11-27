# addon-system Specification

## ADDED Requirements

### Requirement: Addon Consumable and Status Effect Registration
The system SHALL expose `ConsumeEffectRegistry`, `StatusEffectRegistry`, and `PlayerEffectService` on `AddonContext` and track all addon-registered consume effect handler keys and custom status effect keys under the owning addon's identifier.

#### Scenario: Track addon-registered consume and status effects
- **WHEN** an enabled addon registers a custom consume effect handler or custom status effect via its `AddonContext` registries
- **THEN** the system records ownership of those keys under the addon's unique identifier and preserves them across core reloads

## MODIFIED Requirements

### Requirement: Safe 5-Phase Decommissioning Protocol
The system SHALL execute a five-phase decommissioning protocol upon addon disable or unload, ensuring complete resource cleanup and Metaspace leak prevention.

#### Scenario: Disabling an active addon
- **WHEN** an administrator disables an active addon
- **THEN** the system executes `onDisable()`, unregisters all Bukkit listeners, terminates managed executor tasks, removes or deactivates its registry entries (including species, items, crafting recipes, recipes, recipe categories, processes, stations, menus, interactions, consume effect handlers, and custom status effects), purges active player status effects belonging to the removed definitions, unregisters Bukkit crafting recipes, and transitions the addon state to `LOADED`

#### Scenario: Unloading an addon completely
- **WHEN** an administrator unloads an addon
- **THEN** the system disables the addon if active, severs all classloader graph references, closes the `URLClassLoader`, and transitions the addon state to `ON_DISK`

#### Scenario: Handling active world crops for disabled addon species
- **WHEN** growth simulation ticks a crop in the world whose species belongs to a currently disabled addon
- **THEN** the system marks the crop dormant and skips the growth cycle without throwing exceptions or corrupting chunk state
