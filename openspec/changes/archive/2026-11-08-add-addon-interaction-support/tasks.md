# Tasks

## 1. Addon Context Tracking and Decommissioning

- [x] 1.1 Add `registeredInteractions` set, `trackInteraction(Key key)`, and `getRegisteredInteractions()` to `PaperAddonContext` and verify compilation
- [x] 1.2 Update `AddonDecommissioner` (Phase 4: Domain Registry Purging) to unregister tracked interaction keys from `InteractionRegistry` and verify compilation
- [x] 1.3 Add unit tests in `PaperAddonContextTest` and `AddonDecommissionerTest` verifying interaction tracking and decommissioning unregistration

## 2. Declarative Interaction Discovery in PaperAddonManager

- [x] 2.1 Implement `interactions/` scanning using `YamlInteractionLoader.loadMultiple()` in `PaperAddonManager.loadDirectoryContent()` and register/track keys
- [x] 2.2 Implement `interactions/` scanning using `YamlInteractionLoader.loadMultiple()` in `PaperAddonManager.loadJarContent()` and register/track keys
- [x] 2.3 Add unit tests in `PaperAddonManagerTest` verifying directory and JAR interaction discovery, tracking, and purge on disable

## 3. Verification Gate

- [x] 3.1 Run `.\gradlew.bat test` across all modules to ensure zero compilation errors and passing test suites
