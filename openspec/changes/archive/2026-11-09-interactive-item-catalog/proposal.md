# Proposal

## Why

Croppy provides rich declarative item definitions across multiple categories (seeds, fertilizers, watering cans, tools, ingredients, stations, misc) as well as addon-injected items. However, administrative management and testing currently rely entirely on chat-based text commands (`/croppy item list` and `/croppy item give`), requiring administrators to remember or type exact namespaced keys and categories.

Introducing an interactive, menu-driven item catalog accessible via `/croppy items` equips server administrators with a modern visual creative-style spawner (similar to `/sf items`). It simplifies item discovery, rapid testing, and distribution through intuitive click actions, dynamic category tabs, horizontal category scrolling, and integrated text search.

## What Changes

- Introduce a new interactive GUI menu engine for item browsing and administrative spawning governed by `ItemCatalogController` and defined in `items_menu.yml`.
- Add the root subcommand `/croppy items [category] [query]` registered via Incendo Cloud Paper 2.0, protected by permission `croppy.admin.items`, while preserving singular `/croppy item` for CLI and console operations.
- Implement specialized item click interactions in the catalog:
  - **Left-Click**: Give 1 unit (`1x`) directly to player inventory (or drop at feet if full) with audio feedback.
  - **Right-Click**: Give a full stack (`64x` or maximum stack size).
  - **Shift + Click**: Send an interactive `/croppy item give` suggest-command component to player chat.
  - **Drop [Q] / Control + Drop**: Drop 1 unit (or a full stack) naturally on the ground at the player's position.
- Dynamically augment item lore in the catalog with administrative shortcut instructions while preserving the underlying item definition's original display name and lore.
- Establish an extensible `ItemCategoryRegistry` port in `croppy-api` with a thread-safe default implementation in `croppy-core`, pre-registering default categories (`all`, `seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`) and supporting custom categories registered by addons.
- Support horizontal pagination for category tabs (navigation buttons `«` and `»`) when registered categories exceed available header slots (7 slots per page).
- Support dynamic text query searching powered by `ChatPromptService` to filter items in real time across display names and identifiers.
- Expose `ItemCategoryRegistry` in `AddonContext` and ensure safe teardown during addon decommissioning.

## Capabilities

### New Capabilities
- `item-catalog`: Provides an interactive, YAML-driven item catalog GUI for browsing and administrative spawning with multi-action click handlers, category pagination, search filtering, and localized audio/message feedback.

### Modified Capabilities
- `crop-commands`: Add `/croppy items [action] [query]` command dispatch with permission `croppy.admin.items`, maintaining `/croppy item` for console/CLI commands.
- `item-registry`: Establish the `ItemCategoryRegistry` port and `ItemCategory` model representing category metadata (key, icon material, item model, display name, priority).
- `addon-system`: Expose `ItemCategoryRegistry` via `AddonContext` with lifecycle tracking and cleanup upon addon decommissioning.

## Impact

- **API (`croppy-api`)**: New types `ItemCategory` and `ItemCategoryRegistry`.
- **Core (`croppy-core`)**: `DefaultItemCategoryRegistry` implementation, integrated into `CroppyCore` registry facade.
- **Paper (`croppy-paper`)**:
  - New `ItemCatalogController` handling sessions, page calculations, and item interactions.
  - New menu layout resource `menus/items_menu.yml`.
  - Updated `PaperMenuListener` to intercept item clicks in `items_menu`.
  - Updated `CroppyCommands` with `/croppy items` subcommands.
  - Updated `PaperAddonContext` and decommissioning protocol for item category tracking.
  - Localized messages in `en-US.yml` and `es-ES.yml` for catalog interactions, tooltips, and search prompts.
