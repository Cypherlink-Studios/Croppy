# Design: Native Item Models Support (Minecraft 1.21.4+)

## Context

Minecraft 1.21.4+ and Paper 1.21.8+ introduced the native `item_model` component on items, accessible via `ItemMeta#setItemModel(NamespacedKey)`. This replaces the need for custom model data predicate tables in client resource packs. Croppy's hexagonal architecture separates pure domain models (`croppy-api`), configuration loading (`croppy-core`), and Paper platform bindings (`croppy-paper`).

See `proposal.md` for motivation and high-level requirements.

## Goals / Non-Goals

**Goals:**
- Add `Key itemModel` to `ItemDefinition` in `croppy-api` while maintaining binary and source compatibility via an overloaded constructor.
- Support `item-model` and `item_model` keys in `YamlItemLoader` (`croppy-core`).
- Apply `meta.setItemModel(NamespacedKey)` in `ItemFactory` (`croppy-paper`) whenever `itemModel` is present.
- Support dual configuration where both `custom-model-data` and `item-model` can coexist on an item.
- Provide comprehensive JUnit 5 tests verifying loader parsing and item stack construction.

**Non-Goals:**
- Providing an internal resource pack generator, HTTP server, or asset distributor in Croppy (resource pack distribution remains an external concern).
- Modifying `PersistentDataContainer` tag keys or storage serialization.

## Decisions

### 1. Model `itemModel` as Domain `Key` in `croppy-api`
* **Choice**: Use Croppy's pure `dev.darkblade.croppy.api.identifier.Key` record rather than a raw String or Bukkit `NamespacedKey`.
* **Rationale**: Maintains zero Bukkit dependencies in `croppy-api`. The platform adapter in `croppy-paper` converts the `Key` to Bukkit's `NamespacedKey` at the infrastructure boundary.
* **Alternatives considered**: Raw String - rejected because `Key` provides validation and type safety. Bukkit `NamespacedKey` - strictly prohibited by architectural rules (`AGENTS.md`).

### 2. Overloaded Record Constructor for Backward Compatibility
* **Choice**: Retain the original 7-parameter constructor as an overload:
  ```java
  public ItemDefinition(
      Key key, String category, String materialKey, int customModelData,
      String displayName, List<String> lore, Map<String, String> pdcProperties
  ) {
      this(key, category, materialKey, customModelData, null, displayName, lore, pdcProperties);
  }
  ```
  And provide a convenience getter:
  ```java
  public Optional<Key> getItemModel() {
      return Optional.ofNullable(itemModel);
  }
  ```
* **Rationale**: Guarantees existing test suites, callers, and plugins compiling against `croppy-api` continue to work without modification.

### 3. Dual Compatibility Pipeline in `ItemFactory`
* **Choice**: When building `ItemStack`:
  ```java
  if (definition.customModelData() > 0) {
      meta.setCustomModelData(definition.customModelData());
  }
  if (definition.itemModel() != null) {
      meta.setItemModel(new NamespacedKey(definition.itemModel().namespace(), definition.itemModel().value()));
  }
  ```
* **Rationale**: Allows administrators to migrate gradually or serve packs supporting both legacy CMD and modern Data Components.

## Risks / Trade-offs

- **[Risk] Invalid Key format in YAML `item-model`** → *Mitigation*: Catch `IllegalArgumentException` in `YamlItemLoader`, log a clear warning with file context, and leave `itemModel` as null instead of crashing the server initialization.
- **[Risk] Paper runtime missing `setItemModel` on older forks** → *Mitigation*: Croppy targets Paper 1.21.8+ toolchain where `ItemMeta#setItemModel` is guaranteed in the platform API.
