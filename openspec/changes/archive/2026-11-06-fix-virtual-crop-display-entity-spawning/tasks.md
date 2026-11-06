# Tasks

## 1. Packet Dispatch Typing & Safety

- [x] 1.1 Update `PacketEventsBlockRenderer.sendPacket` to accept `PacketWrapper<?>` instead of `Object`, adding null and online player checks, and verify compilation.
- [x] 1.2 Update `VirtualBlockDisplayTracker` to ensure viewer state registration is resilient against dispatch failures and doesn't permanently lock out viewers.

## 2. Immediate Player Join Evaluation

- [x] 2.1 Add `@EventHandler` for `PlayerJoinEvent` in `StationPlayerConnectionListener` triggering immediate proximity evaluation for active block displays and station displays upon player connection.

## 3. Unit Testing & Verification Gate

- [x] 3.1 Create `PacketEventsBlockRendererTest` using mock PacketEvents PlayerManager verifying that `spawnBlockDisplay`, `updateBlockDisplay`, and `destroyBlockDisplay` dispatch properly typed `PacketWrapper<?>` packets.
- [x] 3.2 Execute `./gradlew test` across all modules and verify 100% test pass rate with zero errors.
