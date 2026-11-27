---
title: Puertos, Logging y Registros
description: Referencia para Registry<T>, registros de dominio en AddonContext, canales de CroppyLogger y puertos SPI (ProtectionPort, EconomyPort, TranslationProvider).
sidebar:
  order: 5
---

# Puertos, Logging y Registros de Dominio

Esta referencia documenta el contrato genérico de registros de Croppy, los registros de dominio accesibles desde `AddonContext`, el puerto de logging estructurado `CroppyLogger` y los puertos SPI que conectan integraciones externas y localización.

---

## 1. Contrato Genérico `Registry<T extends Keyed>` (`dev.darkblade.croppy.api.registry`)

Todos los registros de dominio implementan `Registry<T extends Keyed>`:

| Método | Firma | Descripción |
|---|---|---|
| `register` | `void register(T entry)` | Registra una entrada `Keyed` (lanza `IllegalStateException` si la clave está duplicada). |
| `get` | `Optional<T> get(Key key)` | Obtiene una entrada por clave sin devolver `null`. |
| `contains` | `boolean contains(Key key)` | Comprueba si una clave está registrada. |
| `values` / `stream` | `Collection<T>` / `Stream<T>` | Vista inmutable o stream de todas las entradas. |
| `size` / `isEmpty` | `int` / `boolean` | Cantidad actual de entradas. |
| `unregister` | `boolean unregister(Key key)` | Elimina una entrada por su clave. |
| `unregisterAll` | `int unregisterAll(Predicate<T> filter)` | Elimina en lote las entradas que cumplan el predicado (usado en la Fase 4 del desmantelamiento). |
| `clear` | `void clear()` | Vacía todas las entradas registradas. |

---

## 2. Registros de Dominio en `AddonContext`

Los addons habilitados pueden consultar o registrar definiciones de dominio mediante `AddonContext`:

| Accesor en `AddonContext` | Interfaz de Registro | Paquete |
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

Adicionalmente, `DataTypeRegistry` (`api.variable`) expone `getTyped(Key key, Class<T> valueClass)` para contratos de variables tipadas.

> [!IMPORTANT]
> Cuando registres entradas de forma programática desde un addon Java, rastrea cada `Key` registrada mediante `PaperAddonContext#trackSpecies`, `trackItem`, `trackRecipe`, etc., para que el protocolo de desmantelamiento en 5 fases las elimine limpiamente al deshabilitar o descargar el addon.

---

## 3. Logging Estructurado (`CroppyLogger`)

`dev.darkblade.croppy.api.logging.CroppyLogger` desacopla el logging del dominio respecto a JUL/Paper y evita construir cadenas cuando debug o trace están desactivados:

* **Niveles de severidad (`LogLevel`)**: `TRACE` (0), `DEBUG` (1), `INFO` (2), `WARN` (3), `ERROR` (4) (`isAtLeast`, `allows`, `fromString`).
* **Canales de subsistema (`LogChannel`)**: `CORE`, `LOADER`, `GROWTH`, `STATION`, `DISPLAY`, `PACKET`, `STORAGE`, `ADDON`, `PACK`, `INTEGRATION`, `UPDATE`, `I18N`, `COMMAND`.
* **Suppliers perezosos**: Sobrecargas como `debug(LogChannel.ADDON, () -> "Cargados " + count)` invocan el `Supplier<String>` únicamente cuando `isEnabled(level, channel)` es `true`.
* **Loggers hijos acotados**: `logger.scoped("Addon:" + id)` prefija todos los mensajes emitidos con `[<scope>] `.
* **Acceso desde addons**: `context.getCroppyLogger()` devuelve un `CroppyLogger` acotado; las llamadas a `context.getLogger()` (JUL) se redirigen automáticamente al mismo flujo estructurado en Paper.

---

## 4. Puertos SPI

### Protección (`ProtectionPort` y `ProtectionAction`)

`dev.darkblade.croppy.api.protection.ProtectionPort` abstrae la protección de regiones y claims (implementado por `WorldGuardProtectionAdapter` o un respaldo `NoOp`; ver [Integraciones de Terceros](../integrations.md)):

```java
boolean allowed = protectionPort.canInteract(
        playerUuid,
        BlockPosition.of("world", x, y, z),
        ProtectionAction.HARVEST
);
```

Valores de `ProtectionAction`: `PLANT`, `HARVEST`, `BREAK`, `WATER`, `FERTILIZE`, `STATION_USE`, `STATION_FUEL`.

### Economía (`EconomyPort`)

`dev.darkblade.croppy.api.economy.EconomyPort` abstrae transacciones monetarias (implementado por `VaultEconomyAdapter` o `NoOpEconomyAdapter`):

* `boolean has(UUID playerId, double amount)`
* `boolean withdraw(UUID playerId, double amount)`
* `boolean deposit(UUID playerId, double amount)`
* `String format(double amount)`

### Localización (`TranslationProvider`, `MessageKey`, `LocaleRef`)

`dev.darkblade.croppy.api.i18n.TranslationProvider` consulta plantillas MiniMessage crudas a través de la cadena de respaldo de 5 niveles de Croppy (ver [Localización e Internacionalización](../localization.md)):

* `Optional<String> getTemplate(LocaleRef locale, MessageKey key)`
* `boolean hasTranslation(LocaleRef locale, MessageKey key)`
* `Set<LocaleRef> getSupportedLocales()` / `LocaleRef getDefaultLocale()`
* **`MessageKey`**: Record inmutable `<namespace>:<key>` (`MessageKey.of("croppy", "inspector.title")` o `MessageKey.of("inspector.title")`).
* **`LocaleRef`**: Record normalizado de idioma/país BCP-47 (`LocaleRef.EN_US`, `LocaleRef.ES_ES`, `LocaleRef.parse("es-MX")`, `baseLanguage()`, `toLanguageTag()`).

En Paper, los placeholders dinámicos no confiables pasados a `PaperMessageService.arg(key, value)` o `PaperMessageService.args(...)` se envuelven mediante `Placeholder.unparsed(...)` para prevenir inyección de etiquetas MiniMessage, mientras que el formato enriquecido emplea argumentos `Component` de Adventure preconstruidos.
