# Tasks

## 1. Addon Context Tracking and Decommissioning

- [x] 1.1 Add `registeredMenus` set, `trackMenu(Key key)`, and `getRegisteredMenus()` to `PaperAddonContext` and verify compilation
- [x] 1.2 Update `AddonDecommissioner` Phase 4 to purge tracked menus from `core.getMenuRegistry()` and verify compilation
- [x] 1.3 Update `PaperAddonContextTest` and `AddonDecommissionerTest` to verify menu tracking and unregistration

## 2. Declarative Menu Discovery in PaperAddonManager

- [x] 2.1 Implement `menus/` directory scanning in `PaperAddonManager.loadDirectoryContent()` using `YamlMenuLoader` and track keys
- [x] 2.2 Implement `menus/` entry scanning in `PaperAddonManager.loadJarContent()` using `YamlMenuLoader` and track keys
- [x] 2.3 Add unit tests in `PaperAddonManagerTest` asserting menus from directories and JAR archives load into `MenuRegistry` and context

## 3. Full Verification

- [x] 3.1 Execute `./gradlew test` across all modules and confirm 0 test failures
