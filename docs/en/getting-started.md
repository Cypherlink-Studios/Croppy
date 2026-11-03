---
title: Getting Started
description: Step-by-step installation guide for Croppy on Paper 1.21.8+, covering dependencies, configuration, and your first harvest.
sidebar:
  order: 2
---

# Getting Started with Croppy

This guide walks server administrators and players through setting up **Croppy** on a Paper 1.21.8+ server and completing your first agricultural lifecycle.

---

## 1. System Requirements

Before installing Croppy, verify that your server environment meets the following baseline requirements:

| Component | Minimum Requirement | Recommended |
|---|---|---|
| **Server Platform** | Paper 1.21.8 or Purpur 1.21.8 | Latest Paper 1.21.8+ |
| **Java Runtime** | Java 21 (Temurin / GraalVM) | Java 21 LTS |
| **PacketEvents** | 2.14.0+ (Required Dependency) | Latest Release |
| **Folia Support** | Unsupported | Do not run on Folia |

> **Important**: **PacketEvents** is a mandatory dependency. Croppy utilizes packet-level virtual entity displays for workstations and cooking stations to eliminate persistent ghost entities and guarantee smooth client visual performance.

---

## 2. Installation Steps

1. **Install PacketEvents**:
   Download the official `packetevents-spigot` plugin JAR from the [PacketEvents GitHub Releases](https://github.com/retrooper/packetevents/releases) and place it into your server's `plugins/` directory.

2. **Install Croppy**:
   Download the latest `Croppy-0.3.0.jar` from [Croppy Releases](https://github.com/Cypherlink-Studios/Croppy/releases) and place it into `plugins/`.

3. **Start the Server**:
   Launch your server. During startup, Croppy initializes its default directories, unpacks configuration files, sets up the SQLite storage backend, and registers default species and recipes.

4. **Verify Startup**:
   In your server console, look for confirmation messages:
   ```text
   [Croppy] Registered 4 crop species definitions.
   [Croppy] Registered 3 station definitions.
   [Croppy] Registered recipe definitions into RecipeRegistry.
   [Croppy] Initialized SQLite database connection pool via HikariCP.
   [Croppy] Successfully enabled Croppy v0.3.0.
   ```

---

## 3. Your First Crop: Step-by-Step Walkthrough

Follow these steps in-game to plant, nourish, and harvest your first custom crop.

### Step 1: Obtain Tools and Seeds
Ensure you have the `croppy.command.give` permission and run:
```text
/croppy give @s seed:wheat_seeds 5
/croppy give @s watering_can:copper_can 1
/croppy give @s tool:inspector 1
```

### Step 2: Prepare and Hydrate the Farmland
1. Till a dirt block using any vanilla hoe.
2. Take your **Copper Watering Can** to a nearby water source or cauldron and **Right-Click** to fill it.
3. **Right-Click** the tilled farmland with the watering can to deposit moisture. Farmland hydration will increase up to level 8.

```text
+-----------------------------------------------------------+
|                     HYDRATION INDICATOR                   |
|  [Farmland] Moisture: 8/8  *** (Fully Hydrated)           |
+-----------------------------------------------------------+
```

### Step 3: Plant the Seeds
Hold `Wheat Seeds` and **Right-Click** the hydrated farmland. Croppy will instantiate a tracked crop at that coordinate.

### Step 4: Inspect Crop Progress
Hold the **Soil & Crop Inspector** tool and **Right-Click** the crop. An interactive inspection card displays in chat:

```text
-------- [ Crop Inspection ] --------
Species: Golden Wheat (croppy:wheat)
Growth Stage: 1 / 7 (Seedling)
Growth Progress: [======------------] 35%
Light Level: 14 / 9 (Optimal)
Hydration: 8 / 4 (Hydrated)
Soil N-P-K: 100 / 100 / 100
------------------------------------
```

### Step 5: Harvest
When the crop reaches its final stage (100% progress), **Right-Click** or break the crop. You will receive harvested wheat and fresh seeds to replant.
