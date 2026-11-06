# Design

## Context

Croppy uses a strict Hexagonal Architecture across `croppy-api`, `croppy-core`, and `croppy-paper`. The core domain logic has zero knowledge of the Minecraft/Bukkit platform, while `croppy-paper` provides adapters and runtime listeners.

This change introduces optional third-party integrations (WorldGuard, PlaceholderAPI, Vault) without compromising modular boundaries or creating hard runtime dependencies.

See `proposal.md` for motivation and `specs/` for behavioral contracts.

## Goals / Non-Goals

**Goals:**
- Provide compile-time isolation for third-party libraries across dedicated Gradle subprojects under `croppy-integrations/`.
- Expose pure Java 21 ports (`ProtectionPort`, `EconomyPort`) in `croppy-api` with zero Bukkit or external dependencies.
- Register custom WorldGuard state flags during `CroppyPlugin.onLoad()` and evaluate permissions for both Bukkit block events and PacketEvents virtual 3D displays.
- Provide a responsive PlaceholderAPI expansion with configurable dynamic raycasting (`integrations.papi.raycast-distance`) for live target inspection.
- Provide safe fallback no-op implementations so the plugin runs identically whether integrations are enabled or absent.

**Non-Goals:**
- External item bridges (Oraxen, ItemsAdder, MythicMobs) — planned for a dedicated future proposal.
- Direct RPG skill calculation hooks (mcMMO, AuraSkills) — custom events will enable external scripting without hardcoded adapters in this phase.

## Architecture & Module Layout

```text
Croppy (Root)
├── croppy-api/                                  # Pure Java 21 Ports (ProtectionPort, EconomyPort)
├── croppy-core/                                 # Domain simulation & registries
├── croppy-paper/                                # Main plugin adapter, listeners & packaging
└── croppy-integrations/
    ├── common/                                  # dev.darkblade.croppy.integration.common
    │   ├── CroppyIntegration.java               # Lifecycle contract (onLoad, onEnable, onDisable)
    │   ├── IntegrationManager.java              # Registry & safe dispatch
    │   ├── NoOpProtectionAdapter.java           # Pass-through protection
    │   └── NoOpEconomyAdapter.java              # No-funds economy fallback
    ├── worldguard/                              # dev.darkblade.croppy.integration.worldguard
    │   ├── WorldGuardIntegration.java           # Integration lifecycle
    │   ├── WorldGuardFlags.java                 # Custom state flags registered in onLoad()
    │   └── WorldGuardProtectionAdapter.java     # RegionQuery evaluator
    ├── papi/                                    # dev.darkblade.croppy.integration.papi
    │   ├── PapiIntegration.java                 # Registration lifecycle
    │   └── CroppyPlaceholderExpansion.java      # Dynamic raycast & token resolution
    └── vault/                                   # dev.darkblade.croppy.integration.vault
        ├── VaultIntegration.java                # Service lookup lifecycle
        └── VaultEconomyAdapter.java             # Delegation to net.milkbowl.vault.economy.Economy
```

## Decisions

### Decision 1: Submodule Hierarchy `croppy-integrations/<name>`
- **Rationale**: Isolates heavy external dependencies (EngineHub, ExtendedClip, Vault) to their own compilation units. Classpath leaks and dependency version conflicts cannot bleed into core or api.
- **Alternatives Considered**:
  - *Single `croppy-paper` with soft-dependencies*: Harder to manage transitive dependencies and risks accidental imports in core code.
  - *Single monolithic `croppy-integrations` module*: Blends WorldGuard, PAPI, and Vault dependencies into one compile classpath.

### Decision 2: Pure Java Ports in `croppy-api`
- **Rationale**: `ProtectionPort` and `EconomyPort` use standard Java primitives (`UUID`, `BlockPosition`, `double`, `String`, enums). Listeners and services invoke these ports without coupling to Bukkit or integration APIs.
- **Alternatives Considered**:
  - *Paper-only listener checks*: Placing protection checks solely in Bukkit listeners fails to protect PacketEvents virtual display interactions (which do not trigger Bukkit block events).

### Decision 3: Early Flag Registration via `CroppyPlugin.onLoad()`
- **Rationale**: WorldGuard requires custom `StateFlag` definitions to be registered during the plugin's `onLoad()` phase before WorldGuard finalizes its internal registry.
- **Implementation**: `CroppyPlugin` overrides `onLoad()`, instantiates `IntegrationManager`, and triggers `integrationManager.onLoad()`. If WorldGuard is detected, `WorldGuardFlags` registers:
  - `croppy-plant`
  - `croppy-harvest`
  - `croppy-water`
  - `croppy-fertilize`
  - `croppy-cook`

### Decision 4: Dynamic Raycast for Target Placeholders
- **Rationale**: Real-time crosshair inspection (`player.getTargetBlockExact(raycastDistance)`) enables dynamic HUDs (ActionBar/Scoreboard) as players look around crops and stations without requiring persistent tracking metadata.
- **Configuration**: `integrations.papi.raycast-distance` (default: `5`, max recommended: `10`) keeps raytracing cheap on the main thread or asynchronous PAPI workers.

### Decision 5: Dual-Path Protection Checks
- **Rationale**: Croppy handles both real blocks (farmland, crops, stations) and virtual entities (PacketEvents ItemDisplays and Interaction hitboxes).
- **Implementation**:
  - Bukkit listeners (`CropPlantListener`, `CropBreakListener`, `WateringListener`, `FertilizerListener`, `StationBlockListener`) check `ProtectionPort.canInteract()`.
  - Packet listeners (`VirtualInteractionPacketListener`, `StationVirtualInteractionPacketListener`) check `ProtectionPort.canInteract()` before triggering harvest or opening menus.

## Risks / Trade-offs

- **[Risk] Missing third-party plugin causing `NoClassDefFoundError`**  
  → *Mitigation*: Adapter classes are strictly isolated within their integration submodules and are only referenced or instantiated after `Bukkit.getPluginManager().isPluginEnabled(name)` evaluates to true.
- **[Risk] High-frequency PAPI evaluation causing main thread latency**  
  → *Mitigation*: Raycasting uses Bukkit's fast `getTargetBlockExact()` with a small maximum distance. Target lookups query in-memory `CropManager` and `StationManager` (backed by thread-safe `ConcurrentHashMap`).
- **[Risk] WorldGuard API changes between minor versions**  
  → *Mitigation*: The WorldGuard adapter compiles against WorldGuard 7.0.12 (targeting modern 1.21.x Paper) and uses the standard `WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()`.
