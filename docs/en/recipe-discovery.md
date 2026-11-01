---
title: Recipe Discovery Engine
description: Exploration guide to recipe progression, knowledge states (Locked, Hinted, Unlocked), and unlocking culinary secrets.
sidebar:
  order: 11
---

# Recipe Discovery Engine

Croppy encourages culinary experimentation and player progression through a dynamic **Recipe Discovery Engine**. Rather than handing players every recipe upfront in an open crafting book, culinary secrets are discovered through exploration, farming milestones, and kitchen trials.

---

## 1. Player Knowledge States

For every culinary recipe registered in the server, each player maintains a personalized knowledge state:

```text
+-----------------------------------------------------------------------------+
|                          RECIPE PROGRESSION PIPELINE                        |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [1. LOCKED]                                                                |
|  The recipe is unknown. Displayed as an obscured silhouette or question     |
|  mark in the Culinary Compendium. Recipe details remain secret.             |
|                                     |                                       |
|                                     v (Player fulfills hint criteria)       |
|  [2. HINTED]                                                                |
|  The recipe title and a cryptic culinary hint or partial ingredient list    |
|  appear in the compendium, guiding the chef toward the final combination.   |
|                                     |                                       |
|                                     v (Experimental cook or unlock event)   |
|  [3. UNLOCKED]                                                              |
|  The recipe is fully mastered! Complete ingredient matrices, optimal       |
|  thermal zones, and expected yields are permanently visible.                |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Discovery Criteria & Triggers

Recipes configure unlock criteria that fire automatically during gameplay:

* **Harvest Milestones**: Harvesting a rare agricultural species for the first time reveals recipes utilizing that crop.
* **Ingredient Acquisition**: Picking up or holding key spices, flours, or broths can transition related dishes from `LOCKED` to `HINTED`.
* **Station Experimentation**: If an inquisitive player places the exact valid ingredients for a secret recipe onto a cooking pot or skillet and heats it to the proper temperature, the recipe **immediately unlocks upon completion** (`DiscoveryResult.UNLOCKED_EXPERIMENT`).
* **Cookbook Tomes**: Administrators or quest rewards can grant direct recipe unlocks using `/croppy recipe give <player> <recipe_id>`.

---

## 3. Secret Recipes

Recipes can be flagged as `secret: true` in their configuration:
* Secret recipes are completely hidden from the compendium until discovered—they do not even show an empty slot or mystery silhouette.
* Once a player successfully cooks the secret combination by trial and error, a celebratory sound and title fanfare triggers, permanently recording the achievement in their cookbook.
