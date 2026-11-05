# Minecraft 1.21.8 Item Model Properties Reference

This document catalogs the properties available for `minecraft:condition`, `minecraft:select`, and `minecraft:range_dispatch` in item definitions (`assets/<namespace>/items/<item>.json`).

---

## 1. Boolean Condition Properties (`type: "minecraft:condition"`)

Used with `"on_true"` and `"on_false"` models.

| Property | Description |
|---|---|
| `minecraft:using_item` | Evaluates true if the player is actively using (holding right-click on) the item. |
| `minecraft:damaged` | Evaluates true if the item's damage value is greater than 0. |
| `minecraft:broken` | Evaluates true if the item has only 1 durability point remaining. |
| `minecraft:has_component` | Evaluates true if a given component exists. Requires `"component": "<component_id>"`. |
| `minecraft:fishing_rod/cast` | Evaluates true if the fishing rod is currently cast. |
| `minecraft:bundle/has_selected_item` | Evaluates true if a bundle currently has an item highlighted. |
| `minecraft:custom_model_data` | Reads from the `flags` boolean list of CustomModelData using `"index": <int>`. |

---

## 2. Discrete Select Properties (`type: "minecraft:select"`)

Used with `"cases": [{"when": "<value>", "model": {...}}]` and `"fallback"`.

| Property | Possible Values / Usage |
|---|---|
| `minecraft:custom_model_data` | Reads from the `strings` list of CustomModelData using `"index": <int>`. Matches the exact string. |
| `minecraft:display_context` | Context in which item is rendered: `"none"`, `"thirdperson_lefthand"`, `"thirdperson_righthand"`, `"firstperson_lefthand"`, `"firstperson_righthand"`, `"head"`, `"gui"`, `"ground"`, `"fixed"`. |
| `minecraft:main_hand` | Hand dominance: `"left"`, `"right"`. |
| `minecraft:charge_type` | Crossbow projectile type: `"none"`, `"arrow"`, `"rocket"`. |
| `minecraft:trim_material` | Armor trim material ID (e.g. `"minecraft:copper"`, `"minecraft:gold"`). |
| `minecraft:block_state` | Matches a block state property when holding a block item (requires `"block_state_property"`). |

---

## 3. Numeric Range Dispatch Properties (`type: "minecraft:range_dispatch"`)

Used with `"entries": [{"threshold": <float>, "model": {...}}]` and `"fallback"`.

| Property | Value Range & Scale |
|---|---|
| `minecraft:custom_model_data` | Reads from the `floats` list of CustomModelData using `"index": <int>`. |
| `minecraft:damage` | Item damage proportion: normalized float from `0.0` (undamaged) to `1.0` (fully broken). |
| `minecraft:cooldown` | Item cooldown progress from `0.0` (ready) to `1.0` (just entered cooldown). |
| `minecraft:time` | World time or celestial angle: normalized float `0.0` to `1.0`. Supports `"wobble": true/false`. |
| `minecraft:compass` | Compass heading angle from `0.0` to `1.0`. Target modes: `"spawn"`, `"lodestone"`, `"recovery"`. |
| `minecraft:crossbow/pull` | Crossbow loading progress from `0.0` to `1.0`. |
| `minecraft:use_duration` | Duration in ticks the item has been held in use (requires `"scale": <float>`). |
