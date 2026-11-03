---
title: Culinary Compendium
description: Player guide to the in-game cookbook GUI, Recipe Book physical item, category filtering, and dish inspection.
sidebar:
  order: 14
---

# Culinary Compendium (Cookbook)

The **Culinary Compendium** is Croppy's interactive in-game recipe encyclopedia. It gives players a tactile, beautifully illustrated cookbook to track their culinary journey, review cooking requirements, and organize discovered delicacies.

---

## 1. Accessing the Compendium

Players have two convenient methods to consult their cookbook:

1. **The Physical Recipe Book Item (`croppy:recipe_book`)**:
   * A crafted or rewarded book bound with leather and brass buckles.
   * Simply hold the **Recipe Book** in your main hand and **Right-Click** anywhere in the world to open the menu.
2. **The Player Command**:
   * Execute `/croppy recipes` in chat at any time.
   * You can open directly into a category tab: e.g., `/croppy recipes bakery` or `/croppy recipes soups`.

---

## 2. Compendium Interface & Categories

The compendium is organized as an intuitive 54-slot chest matrix:

```text
+-----------------------------------------------------------+
|                   CULINARY COMPENDIUM                     |
+-----------------------------------------------------------+
| [ALL]  [BAKERY]  [SOUPS]  [COOKING]  [BEVERAGES] [PRESERVES]  |  <-- Category Tabs
|-----------------------------------------------------------|
|                                                           |
|    [Dish 1]    [Dish 2]    [Dish 3]    [Dish 4]   ...     |  <-- Recipe Grid
|    [Dish 5]    [ ????? ]   [Dish 7]    [Dish 8]   ...     |
|                                                           |
|-----------------------------------------------------------|
|  [<< Prev Page]          [Page 1 / 3]       [Next Page >>]|  <-- Navigation
+-----------------------------------------------------------+
```

### Supported Meal Categories
* **Bakery**: Bread loaves, pastries, muffins, pies, and wheat confectionery.
* **Soups & Stews**: Hearty broths, chowders, vegetable medleys, and rich simmering pots.
* **Cooking**: Skillet steaks, roasted vegetables, grilled fish, and entrees.
* **Beverages**: Infused teas, pressed juices, ciders, and refreshing tonics.
* **Preserves**: Jams, pickled vegetables, fermented sauces, and dried fruits.

---

## 3. Inspecting a Recipe Card

Clicking on any unlocked recipe in the grid displays its comprehensive preparation card in lore tooltips:

* **Dish Name & Quality**: Colored title with rarity indicator.
* **Cooking Station**: The designated apparatus required (`Cooking Pot` or `Skillet`).
* **Thermal Zone**: Minimum and maximum temperature range (`120°C - 180°C`).
* **Cooking Duration**: Time required to achieve perfect tenderness (e.g., `12.5 seconds`).
* **Required Ingredients**: Exact list and counts of agricultural products needed.
* **Expected Result**: Output items and potential culinary status buffs.
