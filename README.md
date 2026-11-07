# Croppy

<p align="center">
  <strong>Extensible, modular agriculture, soil simulation, and culinary engine for modern Minecraft Paper servers.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8+-55FF55?style=flat-square&logo=minecraft" alt="Minecraft" />
  <img src="https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=flat-square&logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-orange?style=flat-square" alt="Platform" />
  <img src="https://img.shields.io/badge/Architecture-Hexagonal%20Ports%20%26%20Adapters-blue?style=flat-square" alt="Hexagonal Architecture" />
  <img src="https://img.shields.io/badge/Code%20License-GPL--3.0--or--later-brightgreen?style=flat-square" alt="Code License" />
  <img src="https://img.shields.io/badge/Asset%20License-CC%20BY--NC--SA%204.0-yellow?style=flat-square" alt="Asset License" />
</p>

---

## What is Croppy?

**Croppy** is an advanced agricultural and culinary simulation engine designed specifically for modern Paper 1.21.8+ servers. It replaces basic vanilla farming with a living ecological simulation and bridges raw harvest goods directly into physical cooking workstations and dynamic culinary crafting.

Built on modern Minecraft technical foundations, Croppy does not require client-side mods (Forge/Fabric). It pairs an uncompromised server architecture with the **Croppy Companion Resource Pack** (leveraging Minecraft 1.21.8 item definition assets and PacketEvents virtual displays) to bring fluid 3D visuals and immersive mechanics to standard vanilla clients.

---

## Core Pillars & Mechanics

```text
+-----------------------------------------------------------------------------+
|                               CROPPY HIGHLIGHTS                             |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [1. LIVING AGRICULTURAL SIMULATION]                                        |
|  Crops evaluate sunlight, humidity, ambient temperature, and greenhouse     |
|  enclosures rather than relying on vanilla block tick mechanics.            |
|                                                                             |
|  [2. SOIL NUTRIENT DYNAMICS (N-P-K)]                                        |
|  Farmland holds Nitrogen, Phosphorus, and Potassium reserves that deplete   |
|  during growth cycles and require replenishment via organic fertilizers.    |
|                                                                             |
|  [3. MANUAL & PROGRESSIVE WATERING]                                         |
|  Tiered watering cans (Basic, Copper, Iron, Gold, Netherite) hydrate        |
|  multi-block grids with custom capacities and natural evaporation rates.    |
|                                                                             |
|  [4. MULTI-BLOCK HABITS & REHARVEST REGROWTH]                               |
|  Supports vertical footprint crops (such as 2-block sweet grape trellises)  |
|  with clearance checks, immediate chunk-load visual sync, and regrowth.     |
|                                                                             |
|  [5. 3D PHYSICAL WORKSTATIONS]                                              |
|  Place skillets, stoves, and cooking pots directly in the world with        |
|  virtual 3D block displays, timed simmering, and thermal burn mechanics.    |
|                                                                             |
|  [6. VARIABLE HEAT SOURCES & FUELS]                                         |
|  Stoves and fire pits consume solid fuels to generate temperature curves    |
|  that transfer heat dynamically to overlying cooking stations.              |
|                                                                             |
|  [7. RECIPE DISCOVERY & CULINARY COMPENDIUM]                                |
|  Master an in-game cookbook containing categorized dishes, unlock hidden    |
|  recipes through culinary experimentation, and inspect ingredients.        |
|                                                                             |
|  [8. NATIVE MULTI-LANGUAGE SUPPORT (i18n)]                                  |
|  All messages, action bars, tooltips, and item names render in each         |
|  player's preferred client language using MiniMessage formatting.           |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## Common Use Cases

| Server Archetype | How Croppy Elevates Gameplay |
|---|---|
| **SMP & Survival** | Turns farming into a collaborative lifestyle. Players rotate crops, balance compost, share watering duties, and establish dedicated bakeries or taverns. |
| **RPG & Economy** | Powers rich profession systems (**Farmer**, **Agronomist**, **Chef**, **Tavern Keeper**). Unlocks high-tier dishes with unique status effects and buff durations. |
| **Towny & Civ Servers** | Integrates agricultural supply chains into local economies. Certain biomes and soil compositions favor distinct crop species, encouraging trade. |
| **Modpack-like Experience** | Delivers the visual depth of mods like *Farmer's Delight* on Paper servers without forcing players to install client-side mod loaders. |

---

## Architectural Principles

Croppy follows a strict **Hexagonal Architecture (Ports & Adapters)** across modular Gradle projects:

```
Croppy (Root)
├── croppy-api          # Pure Java 21 contracts, ports, coordinates, and immutable records
├── croppy-core         # Pure Java 21 domain engine, simulation, codecs, and registries
├── croppy-integrations # Modular third-party integration adapters (WorldGuard, PAPI, Vault)
└── croppy-paper   # Paper 1.21.8 adapter, PacketEvents displays, PDC/SQLite, & listeners
```

1. **Zero Bukkit in Core & API**: The domain simulation (`croppy-core`) and public API (`croppy-api`) contain zero references to Bukkit, Paper, or Minecraft internals. This guarantees 100% deterministic unit testing and portable simulation logic.
2. **Ghost-Free Packet Displays**: Virtual station models and crop overlays are handled entirely via **PacketEvents** clientbound packets, preventing phantom entities, chunk clutter, or server lag.
3. **Decoupled Integrations Subsystem**: Region protection (WorldGuard), live placeholder expansion (PlaceholderAPI), and economy bridging (Vault) reside in isolated integration submodules with zero runtime coupling and transparent NoOp fallbacks.
4. **Chunk-Synced Persistent Storage**: Default storage uses Minecraft's native Chunk **PersistentDataContainer (PDC)** with an ultra-compact binary format (`CropBinaryCodec`), ensuring zero external dependencies and seamless chunk lifecycle synchronization. Optional SQLite with HikariCP is also available.
5. **Fail-Fast Startup**: All species, recipes, and station YAML configurations are validated upon server boot to catch errors before players connect.

---

## System Requirements

- **Server Platform**: Paper 1.21.8+ or Purpur 1.21.8+ *(Folia is not supported)*.
- **Java Runtime**: Java 21 LTS (GraalVM or Eclipse Temurin recommended).
- **Required Plugin**:
  - [PacketEvents](https://github.com/retrooper/packetevents) `2.14.0+` (required for virtual packet display projections).
- **Supported Integrations** (Optional):
  - [WorldGuard](https://enginehub.org/worldguard/) `7.0.12+` (Land claims and custom flags: plant, harvest, water, fertilize, cook).
  - [PlaceholderAPI](https://placeholderapi.com/) `2.11.6+` (Live crop, soil, station, and compendium tokens).
  - [Vault](https://github.com/MilkBowl/VaultAPI) `1.7+` (Culinary recipes and monetary transaction support).
- **Client Requirements**: Standard Minecraft Java Client (1.21.8+) with the **Croppy Companion Resource Pack** loaded.

---

## Quick Start

### 1. Installation
1. Install `packetevents-spigot.jar` into your server's `plugins/` directory.
2. Download the latest `Croppy-paper-0.5.9.jar` from [Releases](https://github.com/Cypherlink-Studios/Croppy/releases) and place it in `plugins/`.
3. Provide the **Croppy Companion Resource Pack** (`resourcepack/`) to your players via `server.properties` or server resource pack hosting.
4. Start your server.

### 2. Common In-Game Commands

#### Player Commands (No permissions required)
```bash
# Open the interactive Culinary Compendium
/croppy recipes

# Open the compendium filtered by category (e.g. bakery, soups, cooking)
/croppy recipes <category>
```

#### Administrative Commands (`croppy.admin`)
```bash
# Spawn custom agricultural items and tools
/croppy item give seeds wheat_seeds 5
/croppy item give watering_cans copper_can 1
/croppy item give tools inspection_tool 1
/croppy item give misc recipe_book 1

# List registered items in a category
/croppy item list [category]

# Inspect targeted crop/soil state (raycast up to 5 blocks)
/croppy inspect
/croppy inspect <x> <y> <z>

# Debugging and soil simulation controls
/croppy water
/croppy fertilize <fertilizer_key>
/croppy setstage <stage_index>
/croppy setprogress <progress_percent>

# Addon and Resource Pack Pipeline controls
/croppy addon list
/croppy addon info <addon_id>
/croppy pack build
/croppy pack send [player]

# Reload configuration and localization catalogs
/croppy reload
```

---

## Survival Gameplay Walkthrough

1. **Till Soil**: Till standard dirt or grass with any vanilla hoe.
2. **Hydrate**: Fill your **Watering Can** at a water source or cauldron, then right-click the farmland. Tiered cans water larger areas (e.g., Copper waters 1x3, Iron waters 3x3).
3. **Plant**: Right-click hydrated farmland with custom seeds (`wheat_seeds`, `carrot_seeds`, `potato_seeds`, `tomato_seeds`, `rice_seeds`, or trellis `grapes_seeds`).
4. **Inspect**: Right-click the crop with the **Inspection Tool** (`croppy:inspection_tool`) to check light level, soil hydration, and N-P-K nutrient reserves.
5. **Harvest**: Break or right-click the crop when mature (100% progress). Trellis crops like grapes will yield produce while retaining their flowering vine stage for ongoing regrowth!
6. **Cook**: Place a **Stove** and light it with solid fuels; place a **Skillet** or **Cooking Pot** on top. Right-click the workstation with ingredients to begin cooking. Real-time holographic text displays will show cooking progress, temperature, and thermal zones.
7. **Compendium**: Browse discovered recipes and required ingredients at any time by right-clicking with the **Recipe Book** (`croppy:recipe_book`) or running `/croppy recipes`.

---

## Planned Ecosystem & Integrations

While Croppy operates independently out of the box, integrations with popular server ecosystems are actively planned on the roadmap:

- 🛡️ **Claim & Protection Systems**: Native hook support for **WorldGuard**, **Lands**, **Towny**, and **GriefPrevention** to enforce farmland trampling, harvesting, and workstation access permissions inside claims.
- 💰 **Economy & Commerce**: **Vault** integration for agricultural trading, seed markets, restaurant commerce, and culinary dish valuation.
- 📊 **PlaceholderAPI**: Placeholder expansion for player agricultural statistics, discovered recipes, and soil telemetry.

---

## Documentation

Comprehensive guides, configuration references, and developer API documentation are available in the repository:

- 📖 **[Getting Started Guide](docs/en/getting-started.md)**: Full walkthrough for server administrators.
- 🌾 **[Crops & Species Configuration](docs/en/crops-and-species.md)**: YAML specification for custom plants and stages.
- 🧪 **[Soil, Nutrients & Fertilizers](docs/en/soil-and-fertilizers.md)**: Mechanics of N-P-K depletion and compost.
- 🍳 **[Workstations & Cooking Stations](docs/en/workstations-and-cooking.md)**: Thermal sources, skillets, and pots.
- 🌐 **[Localization & Translations](docs/en/localization.md)**: Internationalization and MiniMessage styling.

---

## Companion Resource Pack & Assets

The client visual assets (3D models, textures, GUI fonts, item definitions) are bundled under `resourcepack/`:
- **Format**: Minecraft Pack Format `64` (Minecraft 1.21.8 strict mode compliant).
- **License**: [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)](resourcepack/LICENSE).

---

## Authors & Licensing

- **Authors & Contributors**: Cypherlink Studios & DarkBladeDev.
- **Public Code License**: The Croppy codebase (Core, API, and platform modules) is licensed under the **[GNU General Public License v3.0 or later (GPL-3.0-or-later)](LICENSE)**.
- **Commercial Alternative**: A separate commercial license pathway may be offered for use cases requiring permissions beyond GPLv3. See **[LICENSING.md](LICENSING.md)** and **[COMMERCIAL-LICENSE.md](COMMERCIAL-LICENSE.md)** for details.
- **Companion Asset License**: Visual and audio assets contained in the companion resource pack are licensed under **[CC BY-NC-SA 4.0](resourcepack/LICENSE)**.
- **Copyright & Third-Party Notices**: See **[COPYRIGHT.md](COPYRIGHT.md)** and **[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)** for full legal declarations and attribution.
