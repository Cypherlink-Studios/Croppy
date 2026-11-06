# Design: Fix Crop Harvest and Seed Drops for Custom Items

## Context

Croppy defines custom ingredients, seeds, tools, and stations in YAML files loaded into `ItemRegistry`. In the Paper platform adapter (`croppy-paper`), `ItemFactory` translates domain `ItemDefinition` records into Bukkit `ItemStack` instances with persistent data container (PDC) tags, MiniMessage components, and item model identifiers.

However, crop harvest and breakage listeners (`VirtualInteractionPacketListener` and `CropBreakListener`) were implemented prior to full `ItemRegistry` unification. They relied on `Material.matchMaterial(itemId)` to resolve yield and seed drops. Because custom items (e.g. `croppy:grapes`, `croppy:grape_seeds`, `croppy:tomato`, `croppy:rice_panicle`) are not vanilla Bukkit materials, `matchMaterial` returns `null`, causing drops to silently disappear while the crop state advances or resets.

See [proposal.md](proposal.md) for background and motivation.

## Goals / Non-Goals

**Goals:**
- Provide a centralized, null-safe method in `ItemFactory` to resolve arbitrary item identifier strings (`croppy:grapes`, `minecraft:wheat`, `wheat`) into Bukkit `ItemStack`s.
- Inject `ItemFactory` into `VirtualInteractionPacketListener` and `CropBreakListener`.
- Ensure all harvest yields, dismantling seed drops, and declarative `DROP_ITEM` actions spawn properly formatted custom or vanilla item stacks.
- Preserve full testability with constructor overloading and mock support.

**Non-Goals:**
- Modifying pure domain models or contracts in `croppy-api` or `croppy-core` (records such as `YieldEntry` and `Species` already store agnostic item ID strings).
- Altering existing YAML schemas or syntax in `crops/*.yml` or `interactions/*.yml`.

## Decisions

### Decision 1: Centralized `resolveItemStack` in `ItemFactory`
Add a unified resolution method:
```java
public Optional<ItemStack> resolveItemStack(String identifier, int amount)
```
- **Resolution Order**:
  1. Try parsing `identifier` as a namespaced `Key` (or defaulting namespace to `croppy`) and query `itemRegistry.get(key)`. If found, call `createItemStack(definition, amount)`.
  2. If absent from `ItemRegistry`, treat as a vanilla Minecraft material (normalize, strip `minecraft:` prefix, and resolve via `Material.matchMaterial`). If valid and non-air, construct `new ItemStack(material, Math.max(1, amount))`.
  3. If unresolvable or invalid, log a diagnostic warning and return `Optional.empty()` to prevent spawning unwanted placeholder blocks (such as `BARRIER`).
- **Rationale**: `ItemFactory` already encapsulates Bukkit item creation, PDC serialization, and skull profile resolution.
- **Alternatives Considered**: Inlining resolution logic in listeners (rejected: creates code duplication, diverges behavior, and impairs unit testability).

### Decision 2: Constructor Dependency Injection with Fallbacks
- Inject `ItemFactory` into `VirtualInteractionPacketListener` and `CropBreakListener`.
- Provide overloaded constructors accepting `ItemFactory` while retaining legacy constructors that instantiate a fallback `new ItemFactory(plugin, core.getItemRegistry())` to ensure seamless backwards compatibility for existing unit tests.

### Decision 3: Drop Spawning in `VirtualInteractionPacketListener` and `CropBreakListener`
- In `VirtualInteractionPacketListener.performHarvest()`:
  - For each `YieldEntry yield : species.getHarvest().yields()`, calculate stack count and call `itemFactory.resolveItemStack(yield.itemId(), count).ifPresent(drops::add)`.
- In `VirtualInteractionPacketListener.performDismantle()`:
  - Call `itemFactory.resolveItemStack(species.getDisplay().seedItemId(), 1).ifPresent(stack -> world.dropItemNaturally(dropLoc, stack))`.
- In `VirtualInteractionPacketListener` action `DROP_ITEM`:
  - Call `itemFactory.resolveItemStack(itemObj.toString(), amount).ifPresent(stack -> world.dropItemNaturally(soundLoc, stack))`.
- In `CropBreakListener.onBlockBreak()`:
  - Resolve mature yields and immature seed drops identically via `itemFactory.resolveItemStack(...)`.

## Risks / Trade-offs

- **[Risk] Typo in YAML item ID leads to silent drop omission**  
  → *Mitigation*: `resolveItemStack` logs a `Level.WARNING` containing the invalid item ID string and species context, alerting server administrators while avoiding unhandled exceptions.
- **[Risk] Multiple drops at single block coordinate bunching together**  
  → *Mitigation*: Preserves standard Bukkit `world.dropItemNaturally()`, ensuring natural physics scatter.
