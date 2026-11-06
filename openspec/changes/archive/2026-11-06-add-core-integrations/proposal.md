# Proposal

## Why

Croppy operates in rich Paper server ecosystems where servers rely on region protection (WorldGuard), text HUD/menu formatting (PlaceholderAPI), and server currencies (Vault). Without native integrations, virtual 3D crop displays and station interactions can bypass land claims, server menus and scoreboards cannot read live agricultural telemetry, and culinary progressions cannot integrate with server economies.

Implementing these integrations within a dedicated, decoupled Gradle module hierarchy (`croppy-integrations/<name>`) ensures that external dependencies do not pollute the pure Java domain (`croppy-api`, `croppy-core`), preventing classpath conflicts while delivering seamless out-of-the-box support for server administrators and configurators.

## What Changes

- **Multi-Module Gradle Architecture**:
  - Add root submodule container `croppy-integrations/`.
  - Add `croppy-integrations:common` containing integration lifecycle contracts (`CroppyIntegration`), `IntegrationManager`, and zero-dependency no-op fallback adapters.
  - Add `croppy-integrations:worldguard` providing WorldGuard 7.x region protection checks and custom state flags (`croppy-plant`, `croppy-harvest`, `croppy-water`, `croppy-fertilize`, `croppy-cook`).
  - Add `croppy-integrations:papi` providing a PlaceholderAPI expansion with configurable dynamic raycasting (`raycast-distance`) for live target crop, soil, station, and compendium data.
  - Add `croppy-integrations:vault` providing Vault 1.7.x economy bridging for culinary unlock costs and transactions.
  - Bundle and shade integration adapters into `croppy-paper` with guarded runtime activation.
- **Pure API Contracts (`croppy-api`)**:
  - Introduce `ProtectionPort` and `ProtectionAction` in `croppy-api` (zero external dependencies).
  - Introduce `EconomyPort` in `croppy-api` (zero external dependencies).
- **Protection Enforcement in Paper Platform**:
  - Update Bukkit listeners (`CropPlantListener`, `CropBreakListener`, `WateringListener`, `FertilizerListener`) to verify `ProtectionPort`.
  - Update PacketEvents virtual interaction listeners (`VirtualInteractionPacketListener`, `StationVirtualInteractionPacketListener`) to verify `ProtectionPort` before processing 3D crop harvesting or cooking interactions.
- **Configuration & Lifecycles**:
  - Add `onLoad()` hook to `CroppyPlugin` to register WorldGuard custom flags prior to WorldGuard flag registry finalization.
  - Add `integrations:` configuration section in `croppy-paper/src/main/resources/config.yml`.

## Capabilities

### New Capabilities
- `claim-protection`: Protection port abstraction and region validation across Bukkit events and PacketEvents virtual displays, including WorldGuard custom state flags and standard flag fallbacks.
- `placeholder-telemetry`: PlaceholderAPI expansion delivering dynamic raycast target inspection of crops, soil chemistry, physical cooking stations, and player culinary compendium progress.
- `economy-service`: Pure Java economy port abstraction and Vault integration adapter supporting transaction validation, withdrawals, deposits, and formatted currency outputs.

### Modified Capabilities
<!-- No requirement changes to existing specs -->

## Impact

- **Build & Dependencies**:
  - `settings.gradle.kts` adds `croppy-integrations:common`, `croppy-integrations:worldguard`, `croppy-integrations:papi`, and `croppy-integrations:vault`.
  - Root `build.gradle.kts` adds repositories for EngineHub (WorldGuard), ExtendedClip (PlaceholderAPI), and JitPack (Vault).
  - `croppy-paper/build.gradle.kts` includes implementation and shadowing of the integration subprojects.
- **Runtime Performance & Safety**:
  - Soft-dependency isolation: third-party plugin classes are only loaded if the respective plugin is detected and enabled.
  - No impact on `croppy-core` (preserves hexagonal purity).
- **Configuration**:
  - New configuration nodes under `integrations.worldguard`, `integrations.papi`, and `integrations.vault` in `config.yml`.
