# Tasks

## 1. Pure API Contracts (`croppy-api`)

- [x] 1.1 Create `AddonConfig` interface in `dev.darkblade.croppy.api.addon` with type-safe getters (`getString`, `getInt`, `getBoolean`, `getDouble`, `getStringList`), section queries (`getSection`, `getKeys`), value mutations (`set`), and persistence hooks (`reload`, `save`), verifying clean compilation.
- [x] 1.2 Update `AddonContext` in `croppy-api` with `getConfig()`, `saveDefaultConfig()`, `reloadConfig()`, and `saveConfig()` signatures.
- [x] 1.3 Verify `croppy-api` compiles cleanly with zero external dependencies via `./gradlew :croppy-api:compileJava`.

## 2. Core Configuration Engine & Path Ownership (`croppy-core`)

- [x] 2.1 Implement `YamlAddonConfig` in `dev.darkblade.croppy.core.addon` using SnakeYAML 2.2 with dot-notation hierarchical lookup, default fallbacks, nested section views, and file persistence.
- [x] 2.2 Implement unit tests in `YamlAddonConfigTest` verifying typed queries, default value fallbacks, nested sections, and save/reload cycles.
- [x] 2.3 Update `DefaultAddonManager` to track `addonRoots` and `dataFolders`, implementing the Path Ownership Gate to detect and reject duplicate addon IDs and cross-occupancy collisions with fail-fast logs.
- [x] 2.4 Implement unit tests in `DefaultAddonManagerCollisionTest` verifying duplicate ID rejection, directory root data folder binding, and cross-occupancy collision rejection.
- [x] 2.5 Verify `croppy-core` compiles and all unit tests pass via `./gradlew :croppy-core:test`.

## 3. Paper Platform Adapter & Extraction Pipeline (`croppy-paper`)

- [x] 3.1 Implement `PaperAddonConfig` in `dev.darkblade.croppy.platform.paper.addon` adapting Bukkit's `YamlConfiguration` to preserve comments and formatting.
- [x] 3.2 Update `PaperAddonContext` to implement `getConfig()`, `saveDefaultConfig()`, `reloadConfig()`, and `saveConfig()`, delegating to `PaperAddonConfig` with fallback to `YamlAddonConfig`.
- [x] 3.3 Update `PaperAddonManager` to bind directory addons directly to their root directory, extract bundled `/config.yml` from JAR archives or directories during `saveDefaultConfig()`, and enforce Path Ownership validation.
- [x] 3.4 Implement unit tests in `PaperAddonContextTest` and `PaperAddonManagerTest` verifying configuration extraction, comment-preserving adapter delegation, and collision rejection.
- [x] 3.5 Verify `croppy-paper` compiles and all tests pass via `./gradlew :croppy-paper:test`.

## 4. End-to-End Verification

- [x] 4.1 Author a default `config.yml` in `Croppy-addons/addons/coffee` and verify `CroppyCoffeeAddon` can invoke `context.saveDefaultConfig()` and query values from `context.getConfig()`.
- [x] 4.2 Execute the full test suite across all subprojects via `./gradlew test` and confirm 100% pass rate.
