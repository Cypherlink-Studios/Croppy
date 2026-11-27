---
title: Ports, Logging & Registries
description: Reference for Registry<T>, AddonContext domain registries, CroppyLogger channels, and SPI ports (ProtectionPort, EconomyPort, TranslationProvider).
sidebar:
  order: 5
---

# Ports, Logging & Domain Registries

This reference documents Croppy's generic registry contract, the domain registries accessible from `AddonContext`, the structured `CroppyLogger` port, and the SPI ports bridging third-party integrations and localization.

---

## 1. Generic `Registry<T extends Keyed>` (`dev.darkblade.croppy.api.registry`)

All domain registries implement `Registry<T extends Keyed>`:

| Method | Signature | Description |
|---|---|---|
| `register` | `void register(T entry)` | Registers a `Keyed` entry (throws `IllegalStateException` on duplicate keys). |
| `get` | `Optional<T> get(Key key)` | Retrieves an entry by key without returning `null`. |
| `contains` | `boolean contains(Key key)` | Checks whether a key is registered. |
| `values` / `stream` | `Collection<T>` / `Stream<T>` | Unmodifiable view or stream of all entries. |
| `size` / `isEmpty` | `int` / `boolean` | Current entry count. |
| `unregister` | `boolean unregister(Key key)` | Removes an entry by key. |
| `unregisterAll` | `int unregisterAll(Predicate<T> filter)` | Batch-removes entries matching a predicate (used during addon Phase 4 purge). |
| `clear` | `void clear()` | Clears all registered entries. |

---

## 2. Domain Registries via `AddonContext`

Enabled addons can query or register domain definitions through `AddonContext`:

| Accessor on `AddonContext` | Registry Interface | Package |
|---|---|---|
| `getSpeciesRegistry()` | `SpeciesRegistry` | `api.species` |
| `getItemRegistry()` | `ItemRegistry` | `api.item` |
| `getItemCategoryRegistry()` | `ItemCategoryRegistry` | `api.item` |
| `getFertilizerRegistry()` | `FertilizerRegistry` | `api.fertilizer` |
| `getRecipeRegistry()` | `RecipeRegistry` | `api.recipe` |
| `getCraftingRecipeRegistry()` | `CraftingRecipeRegistry` | `api.crafting` |
| `getRecipeCategoryRegistry()` | `RecipeCategoryRegistry` | `api.recipe` |
| `getProcessRegistry()` | `ProcessRegistry` | `api.process` |
| `getStationRegistry()` | `StationRegistry` | `api.station` |
| `getInteractionRegistry()` | `InteractionRegistry` | `api.interaction` |
| `getRuleRegistry()` | `RuleRegistry` | `api.rule` |
| `getMenuRegistry()` | `MenuRegistry` | `api.menu` |
| `getItemMetadataRegistry()` | `ItemMetadataRegistry` | `api.item` |
| `getStationActionRegistry()` | `StationActionRegistry` | `api.station` |
| `getConsumeEffectRegistry()` | `ConsumeEffectRegistry` | `api.consumable` |
| `getStatusEffectRegistry()` | `StatusEffectRegistry` | `api.effect` |
| `getPlayerEffectService()` | `PlayerEffectService` | `api.effect` |

`DataTypeRegistry` (`api.variable`) additionally exposes `getTyped(Key key, Class<T> valueClass)` for typed variable contracts.

> [!IMPORTANT]
> When registering custom entries programmatically from a Java addon, track the registered `Key` via `PaperAddonContext#trackSpecies`, `trackItem`, `trackRecipe`, etc., so Croppy's 5-phase decommissioning protocol unregisters them cleanly on disable/unload.

---

## 3. Structured Logging (`CroppyLogger`)

`dev.darkblade.croppy.api.logging.CroppyLogger` decouples domain logging from JUL/Paper and avoids string allocation on disabled debug/trace paths:

* **Severity levels (`LogLevel`)**: `TRACE` (0), `DEBUG` (1), `INFO` (2), `WARN` (3), `ERROR` (4) (`isAtLeast`, `allows`, `fromString`).
* **Subsystem channels (`LogChannel`)**: `CORE`, `LOADER`, `GROWTH`, `STATION`, `DISPLAY`, `PACKET`, `STORAGE`, `ADDON`, `PACK`, `INTEGRATION`, `UPDATE`, `I18N`, `COMMAND`.
* **Lazy suppliers**: Overloads such as `debug(LogChannel.ADDON, () -> "Loaded " + count)` invoke the `Supplier<String>` only when `isEnabled(level, channel)` is `true`.
* **Scoped child loggers**: `logger.scoped("Addon:" + id)` prefixes all emitted messages with `[<scope>] `.
* **Addon access**: `context.getCroppyLogger()` returns a scoped `CroppyLogger`; calls to `context.getLogger()` (JUL) are automatically bridged into the same structured pipeline on Paper.

---

## 4. SPI Ports

### Protection (`ProtectionPort` & `ProtectionAction`)

`dev.darkblade.croppy.api.protection.ProtectionPort` abstracts claim and region protection (implemented by `WorldGuardProtectionAdapter` or a `NoOp` fallback; see [Third-Party Integrations](../integrations.md)):

```java
boolean allowed = protectionPort.canInteract(
        playerUuid,
        BlockPosition.of("world", x, y, z),
        ProtectionAction.HARVEST
);
```

`ProtectionAction` values: `PLANT`, `HARVEST`, `BREAK`, `WATER`, `FERTILIZE`, `STATION_USE`, `STATION_FUEL`.

### Economy (`EconomyPort`)

`dev.darkblade.croppy.api.economy.EconomyPort` abstracts currency transactions (implemented by `VaultEconomyAdapter` or `NoOpEconomyAdapter`):

* `boolean has(UUID playerId, double amount)`
* `boolean withdraw(UUID playerId, double amount)`
* `boolean deposit(UUID playerId, double amount)`
* `String format(double amount)`

### Localization (`TranslationProvider`, `MessageKey`, `LocaleRef`)

`dev.darkblade.croppy.api.i18n.TranslationProvider` queries raw MiniMessage templates across Croppy's 5-tier fallback chain (see [Localization & i18n](../localization.md)):

* `Optional<String> getTemplate(LocaleRef locale, MessageKey key)`
* `boolean hasTranslation(LocaleRef locale, MessageKey key)`
* `Set<LocaleRef> getSupportedLocales()` / `LocaleRef getDefaultLocale()`
* **`MessageKey`**: Immutable `<namespace>:<key>` record (`MessageKey.of("croppy", "inspector.title")` or `MessageKey.of("inspector.title")`).
* **`LocaleRef`**: Normalized BCP-47 language/country record (`LocaleRef.EN_US`, `LocaleRef.ES_ES`, `LocaleRef.parse("es-MX")`, `baseLanguage()`, `toLanguageTag()`).

On Paper, untrusted dynamic placeholders passed to `PaperMessageService.arg(key, value)` or `PaperMessageService.args(...)` are wrapped via `Placeholder.unparsed(...)` to prevent MiniMessage tag injection, while rich formatting uses pre-built Adventure `Component` arguments.
