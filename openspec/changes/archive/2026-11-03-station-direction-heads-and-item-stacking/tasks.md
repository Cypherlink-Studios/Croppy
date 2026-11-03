# Tasks: Dynamic Station Direction, Skull Textures, and Item Stacking Fix

## 1. API Contracts and Domain Models (croppy-api)

- [x] 1.1 Add `getYaw()` and `setYaw(float yaw)` methods with default implementations to `StationInstance` and verify compilation.
- [x] 1.2 Add `RotationMode rotationMode` field to `StationDefinition` defaulting to `RotationMode.CARDINAL_4` and verify constructor overloads.
- [x] 1.3 Add optional `String skullTexture` parameter and `skullTexture()` getter to `ItemDefinition` and verify backwards-compatible constructor.
- [x] 1.4 Update and execute `ItemDefinitionTest` and `StationDefinitionTest` to verify record immutability and default field resolutions.

## 2. Core Domain Simulation & Codecs (croppy-core)

- [x] 2.1 Update `DefaultStationInstance` to store and expose mutable horizontal `yaw` orientation.
- [x] 2.2 Increment `StationBinaryCodec.CURRENT_VERSION` to 4, serialize `yaw` for V4, and ensure V1-V3 legacy payloads deserialize with `0.0f` yaw; verify with `StationBinaryCodecTest`.
- [x] 2.3 Extend `YamlItemLoader` to parse `texture` and `skull-texture` fields (validating base64 and Mojang texture URLs) into `ItemDefinition`; verify with `YamlItemLoaderTest`.
- [x] 2.4 Extend `YamlStationLoader` to parse `rotation_mode` or `rotation` into `StationDefinition`; verify with `YamlStationLoaderTest`.
- [x] 2.5 Run `./gradlew.bat :croppy-core:test` and verify all core unit test suites pass.

## 3. Paper Platform Implementation & Stacking Bugfix (croppy-paper)

- [x] 3.1 Implement `public ItemStack createBukkitItem(StoredItem stored)` in `ItemFactory`, cleanly returning unmodified vanilla `ItemStack` instances for standard materials without synthetic display names or PDC tags.
- [x] 3.2 Refactor `PaperMenuRenderer`, `StationInteractListener`, `StationBlockListener`, and `StationGuiListener` to delegate to `itemFactory.createBukkitItem(stored)`, eliminating duplicate code and fixing vanilla item stacking.
- [x] 3.3 Implement `PlayerProfile` texture creation in `ItemFactory#applyToMeta` using deterministic UUIDs for `minecraft:player_head` items with configured base64 or Mojang URLs.
- [x] 3.4 Update `StationBlockListener.onBlockPlace` to compute placement yaw facing the player via `RotationCalculator`, pass it to `DefaultStationInstance`, and synchronize `PlayerProfile` to in-world `Skull` block states when base block is a head.
- [x] 3.5 Update `PacketEventsStationRenderer` to incorporate station placement yaw into the base model's rotation quaternion and rotate horizontal translation offsets for child process displays.

## 4. Verification & Quality Assurance

- [x] 4.1 Implement `ItemFactoryTest` unit tests asserting that retrieved vanilla items have null custom display names, empty custom PDC, and stack cleanly with untouched vanilla items.
- [x] 4.2 Implement unit tests in `StationBlockBreakDropTest` and `PacketStationDisplayTest` verifying orientation calculation and display transforms with yaw.
- [x] 4.3 Execute full pre-commit verification gate (`.\gradlew.bat test`) ensuring 100% test success across all three modules.
