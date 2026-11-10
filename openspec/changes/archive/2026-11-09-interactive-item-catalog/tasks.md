# Tasks: Interactive Item Catalog

## 1. Domain Ports and Core Registry (`croppy-api` & `croppy-core`)

- [x] 1.1 Create `ItemCategory` immutable record in `croppy-api` with key, icon material, optional item model, display name, and priority, verifying with `ItemCategoryTest.java`.
- [x] 1.2 Create `ItemCategoryRegistry` interface port in `croppy-api` defining registration, lookup, removal, and listing methods.
- [x] 1.3 Implement `DefaultItemCategoryRegistry` in `croppy-core` with thread-safe storage and default categories (`all`, `seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`), verifying with `DefaultItemCategoryRegistryTest.java`.
- [x] 1.4 Expose `ItemCategoryRegistry` in `CroppyCore` registry facade and wire with `DefaultItemRegistry`.

## 2. Menu Layout and Localization Resources

- [x] 2.1 Create `items_menu.yml` in `croppy-paper/src/main/resources/menus/` defining the 6-row matrix layout, category header navigation, 28-item display slots, and footer action buttons.
- [x] 2.2 Add catalog message strings to `en-US.yml` and `es-ES.yml` for category labels, tooltip shortcut hints, search prompts, and item distribution feedback.

## 3. Paper Controller and Interaction Dispatch (`croppy-paper`)

- [x] 3.1 Implement `ItemCatalogController` in `croppy-paper` to manage player sessions (`ItemCatalogSession`), item pagination, category tab filtering, horizontal category scrolling, and dynamic lore augmentation.
- [x] 3.2 Update `PaperMenuListener` to intercept clicks in `items_menu`:
  - Left-Click: give 1x item to inventory (or drop at feet if full).
  - Right-Click: give full stack (up to max stack size).
  - Shift-Click: send interactive suggest-command component `/croppy item give <category> <id> 1` to chat.
  - Drop [Q]: drop item naturally on the ground at player position.
- [x] 3.3 Connect `ChatPromptService` to `ItemCatalogController` for interactive in-game text search with timeout and cancellation.

## 4. Cloud Command and Addon Lifecycle Integration

- [x] 4.1 Register `/croppy items [action] [query]` command in `CroppyCommands.java` with permission node `croppy.admin.items`, retaining singular `/croppy item` for CLI operations.
- [x] 4.2 Expose `ItemCategoryRegistry` in `AddonContext` (`croppy-api`) and `PaperAddonContext` (`croppy-paper`), ensuring registered addon categories are tracked and cleaned up on decommission.
- [x] 4.3 Add unit tests for `ItemCatalogController` session updates, pagination calculations, and filtering logic.

## 5. Verification Gate

- [x] 5.1 Run full Gradle test suite via `.\gradlew.bat test` and verify zero compilation errors and zero test failures across all modules.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result.
