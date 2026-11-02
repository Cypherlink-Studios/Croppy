# Proposal: Native Item Models Support (Minecraft 1.21.4+)

## Why

Minecraft 1.21.4+ (and Paper 1.21.8+) introduced the native `item_model` Data Component (`minecraft:item_model`), allowing custom item textures and 3D models to be assigned directly to items using namespaced keys without relying on legacy `custom_model_data` integer predicates. Currently, Croppy only supports integer `custom_model_data`, requiring server administrators to use complex legacy predicate JSONs.

Supporting both native `item_model` and legacy `custom_model_data` gives administrators full flexibility to use modern resource pack architectures (such as CraftLab or vanilla resource packs) while remaining 100% backward-compatible.

## What Changes

- Extend `ItemDefinition` record in `croppy-api` with an optional `Key itemModel` property, maintaining an overloaded constructor for backward compatibility.
- Update `YamlItemLoader` in `croppy-core` to parse `item-model` and `item_model` from item YAML definitions into a namespaced `Key`.
- Update `ItemFactory` in `croppy-paper` to apply `meta.setItemModel(NamespacedKey)` whenever an `itemModel` is defined, coexisting with `custom_model_data`.
- Add test coverage for parsing, validating, and applying native item models.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `item-registry`: Extend declarative item definition schema and item stack creation to support native `item_model` Data Component keys alongside legacy `custom_model_data`.

## Impact

- **API (`croppy-api`)**: Non-breaking extension to `ItemDefinition` record with an overloaded constructor to preserve binary/source compatibility.
- **Core (`croppy-core`)**: `YamlItemLoader` now recognizes `item-model` and `item_model`.
- **Platform (`croppy-paper`)**: `ItemFactory` calls Paper 1.21.8 `ItemMeta#setItemModel(NamespacedKey)`.
- **Resources**: Item YAMLs can declare `item-model: "croppy:item/..."`.
