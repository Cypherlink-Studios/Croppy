# Tasks

## 1. Pure API Contracts (croppy-api)

- [x] 1.1 Define `RotationMode` enum (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`, `RANDOM_JITTER`) in `dev.darkblade.croppy.api.species` and verify compilation with `./gradlew :croppy-api:classes`.
- [x] 1.2 Define `Model3DDefinition` immutable record in `dev.darkblade.croppy.api.species` modeling item model key, scale vectors, translation vectors, and hitbox width/height, and verify compilation with `./gradlew :croppy-api:classes`.
- [x] 1.3 Update `CropInstance` interface adding `float getYaw()` and `void setYaw(float yaw)` with default methods, and verify compilation with `./gradlew :croppy-api:classes`.
- [x] 1.4 Update `StageDefinition` record to include optional `Model3DDefinition` with backward-compatible constructors and verify compilation with `./gradlew :croppy-api:classes`.

## 2. Domain Core Logic & Persistence (croppy-core)

- [x] 2.1 Implement `RotationCalculator` utility for snapping angles to cardinal/8-way directions and quaternion generation, and verify with unit tests in `RotationCalculatorTest`.
- [x] 2.2 Update `DefaultCropInstance` to store and expose `yaw` orientation, and verify existing unit tests pass.
- [x] 2.3 Upgrade `CropBinaryCodec` to `VERSION_3` serializing 4-byte float yaw, and verify V3 round-trip and V1/V2 backward compatibility in `CropBinaryCodecTest`.
- [x] 2.4 Update `YamlSpeciesLoader` to parse `display.type: model_3d`, `rotation_mode`, and stage-specific `model` and `hitbox` mappings, and verify with unit tests in `YamlSpeciesLoaderTest`.

## 3. Virtual Packet Rendering & Click Interception (croppy-paper)

- [x] 3.1 Create `VirtualBlockDisplay` state container tracking block position, `ItemDisplay` entity ID, and `Interaction` entity ID with player viewer sets.
- [x] 3.2 Implement `PacketEventsBlockRenderer` to dispatch spawn, metadata, and destroy packets for `ITEM_DISPLAY` and `INTERACTION` entities with yaw quaternions.
- [x] 3.3 Implement `VirtualInteractionPacketListener` intercepting `WrapperPlayClientInteractEntity` packets and routing `ATTACK` and `INTERACT` to the main thread.
- [x] 3.4 Update `PaperDisplaySynchronizer` to invoke `PacketEventsBlockRenderer` when crop species display type is `model_3d` during stage transitions.
- [x] 3.5 Implement crop planting listener to capture player yaw, calculate rotation based on species `RotationMode`, and register the new crop with orientation.
- [x] 3.6 Implement `BlockPlaceEvent` safeguard listener preventing block placement collisions inside coordinates occupied by active 3D crops.

## 4. End-to-End Verification & Verification Gate

- [x] 4.1 Author sample 3D crop species definition (`tomato.yml`) demonstrating `model_3d` configuration with stage models and hitboxes.
- [x] 4.2 Execute full `./gradlew test` test suite ensuring all API, Core, and Paper unit tests pass with zero failures.
