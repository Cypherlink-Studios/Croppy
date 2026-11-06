# Tasks

## 1. API Ports & Contracts (`croppy-api`)

- [x] 1.1 Implement `ProtectionPort` and `ProtectionAction` in `croppy-api` and verify via unit tests that coordinates and actions model correctly without Bukkit imports
- [x] 1.2 Implement `EconomyPort` in `croppy-api` and verify via unit tests that financial queries, transactions, and currency formatting contracts compile cleanly

## 2. Multi-Module Scaffolding & Common Integration Framework

- [x] 2.1 Update `settings.gradle.kts` and root `build.gradle.kts` to register external repositories (EngineHub, ExtendedClip, JitPack) and include `croppy-integrations:common`, `croppy-integrations:worldguard`, `croppy-integrations:papi`, and `croppy-integrations:vault`, verifying Gradle project evaluation succeeds
- [x] 2.2 Implement `CroppyIntegration` lifecycle interface and `NoOpProtectionAdapter` / `NoOpEconomyAdapter` fallbacks in `croppy-integrations:common`, and verify behavior via unit tests
- [x] 2.3 Implement `IntegrationManager` in `croppy-integrations:common` to manage registration, safe availability checks, and lifecycle dispatch (`onLoad`, `onEnable`, `onDisable`), and verify dispatch via unit tests

## 3. WorldGuard Protection Integration (`croppy-integrations:worldguard`)

- [x] 3.1 Configure dependencies in `croppy-integrations/worldguard/build.gradle.kts` with WorldGuard 7.x and WorldEdit dependencies
- [x] 3.2 Implement `WorldGuardFlags` defining and registering custom flags (`croppy-plant`, `croppy-harvest`, `croppy-water`, `croppy-fertilize`, `croppy-cook`) during the `onLoad` phase
- [x] 3.3 Implement `WorldGuardProtectionAdapter` querying WorldGuard `RegionContainer` and testing custom flags with fallback to `Flags.BUILD` and `Flags.INTERACT`
- [x] 3.4 Implement `WorldGuardIntegration` implementing `CroppyIntegration` to coordinate registration and availability checks

## 4. PlaceholderAPI Telemetry Integration (`croppy-integrations:papi`)

- [x] 4.1 Configure dependencies in `croppy-integrations/papi/build.gradle.kts` with PlaceholderAPI compileOnly dependency
- [x] 4.2 Implement `CroppyPlaceholderExpansion` handling dynamic raycasting (`raycast-distance`) and parsing crop, soil, workstation, compendium, and global statistics tokens, verifying token parsing logic with unit tests
- [x] 4.3 Implement `PapiIntegration` implementing `CroppyIntegration` to manage expansion registration and unregistration

## 5. Vault Economy Integration (`croppy-integrations:vault`)

- [x] 5.1 Configure dependencies in `croppy-integrations/vault/build.gradle.kts` with VaultAPI compileOnly dependency
- [x] 5.2 Implement `VaultEconomyAdapter` wrapping `net.milkbowl.vault.economy.Economy` and delegating `EconomyPort` operations safely
- [x] 5.3 Implement `VaultIntegration` implementing `CroppyIntegration` hooking into Bukkit `ServicesManager` on enable

## 6. Platform Wiring & Protection Enforcement in `croppy-paper`

- [x] 6.1 Update `croppy-paper/build.gradle.kts` to depend on and shadow all `croppy-integrations:*` submodules
- [x] 6.2 Update `config.yml` and `PluginConfig.java` with the `integrations:` configuration section for WorldGuard, PAPI (`raycast-distance: 5`), and Vault
- [x] 6.3 Implement `CroppyPlugin.onLoad()` and `onEnable()` hooks to initialize `IntegrationManager` and register active adapters
- [x] 6.4 Enforce `ProtectionPort` checks in Bukkit listeners (`CropPlantListener`, `CropBreakListener`, `WateringListener`, `FertilizerListener`, `StationBlockListener`)
- [x] 6.5 Enforce `ProtectionPort` checks in PacketEvents packet listeners (`VirtualInteractionPacketListener`, `StationVirtualInteractionPacketListener`) for 3D crops and workstations

## 7. Verification & Build Validation

- [x] 7.1 Execute `./gradlew.bat test` across all modules to verify 100% test pass rate with zero dependency leaks
- [x] 7.2 Execute `./gradlew.bat shadowJar` and verify that the shaded `Croppy-paper.jar` bundles integration adapters cleanly without missing class definitions
