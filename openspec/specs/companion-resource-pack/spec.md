# companion-resource-pack Specification

## Purpose
Provides an official Minecraft Java 1.21.8 companion resource pack (pack_format 64) for Croppy, managing item model definitions, negative-space GUI fonts, spatial audio, and asset quality validation.

## Requirements

### Requirement: Canonical 1.21.8 Pack Metadata
The companion resource pack SHALL declare root configuration in `pack.mcmeta` specifying integer `pack_format: 64` and `supported_formats` bounds of 63 to 64. It MUST NOT use decimal or floating format specifiers.

#### Scenario: Validating pack format
- **WHEN** a Minecraft 1.21.8 client inspects `pack.mcmeta`
- **THEN** the client recognizes the pack format as 64 without compatibility warnings

### Requirement: Namespaced Asset Hierarchy and Path Compliance
All resource pack assets SHALL be contained under `assets/<namespace>/`, where `<namespace>` is `croppy` or any registered addon namespace. Every directory and filename within the assets tree MUST strictly adhere to the lowercase regex `^[a-z0-9_.-]+$`.

#### Scenario: Auditing asset paths
- **WHEN** the asset directory tree is traversed across core and addon namespaces
- **THEN** all path segments contain only lowercase alphanumeric characters, dots, underscores, and dashes

#### Scenario: Supporting multi-namespace asset trees
- **WHEN** an addon declares namespace "winery" and provides assets in `assets/winery/`
- **THEN** the system validates paths under `assets/winery/` against regex `^[a-z0-9_.-]+$` without requiring assets to live under `assets/croppy/`

### Requirement: Modern Item Model Definitions
The resource pack SHALL declare custom item models inside `assets/croppy/items/<item>.json` utilizing modern dispatch trees (`minecraft:model`, `minecraft:select`, `minecraft:range_dispatch`), providing synchronized item definitions for neutral vanilla-tiered tools, crops, and workstations without orphaned, unreferenced, or non-neutral assets. Item models MUST NOT declare legacy `overrides` arrays in `models/item/`.

#### Scenario: Resolving item definition for custom items
- **WHEN** a player holds an item stack whose `item_model` component points to a registered item definition
- **THEN** the client resolves the model geometry via `assets/croppy/items/<item>.json` matching the valid item model target

#### Scenario: Validating model definitions for watering can tiers
- **WHEN** the resource pack compiles item definitions for watering cans
- **THEN** item models for native Minecraft material tiers (`wooden_can`, `copper_can`, `iron_can`, `golden_can`, `diamond_can`, `netherite_can`) SHALL exist under `assets/croppy/items/`

### Requirement: Negative-Space Unicode Font Providers
The resource pack SHALL define font providers in `assets/croppy/font/default.json` with a `space` provider declaring binary negative advances (`\uF801` through `\uF880`).

#### Scenario: Shifting inventory titles for borderless GUIs
- **WHEN** a menu title contains `\uF808` followed by a custom glyph
- **THEN** the rendered cursor shifts 8 pixels to the left to align with the inventory boundary

### Requirement: Automated Strict Quality Audit Gate
The companion resource pack SHALL pass the `audit_pack_1218.py` validation script with zero critical errors, guaranteeing strict JSON parsing compliance without trailing commas or unquoted keys.

#### Scenario: Running pre-commit asset audit
- **WHEN** `audit_pack_1218.py` evaluates the `resourcepack/` root
- **THEN** the script terminates with exit code 0 and zero critical errors

### Requirement: Normalized Model UV Bounds
The companion resource pack SHALL declare all element face UV coordinates in custom block and item models within the normalized bounds of 0.0 to 16.0 on both axes, preventing texture atlas oversampling and adjacent sprite bleeding across multi-block heights and non-square textures.

#### Scenario: Validating model face UV coordinates
- **WHEN** a custom block or item model JSON in `assets/croppy/models/` is evaluated
- **THEN** all UV coordinate values across every element face are greater than or equal to 0.0 and less than or equal to 16.0

#### Scenario: Rendering multi-block crop stages
- **WHEN** a 2-block crop model with a 16x32 texture renders in the world
- **THEN** the model samples the full vertical sprite within the normalized 0.0 to 16.0 UV range without bleeding into neighboring atlas textures

### Requirement: Automated Addon Asset Merging
The companion resource pack compiler SHALL discover `assets/` subdirectories inside enabled addons and merge their models, textures, and sounds into the compiled companion resource pack output file at `plugins/Croppy/exports/croppy-unified-pack.zip`, computing its SHA-1 checksum upon completion.

#### Scenario: Merging addon models and textures
- **WHEN** an addon containing `assets/custom/models/item/wand.json` is enabled
- **THEN** the compiled resource pack output includes `assets/custom/models/item/wand.json` alongside base Croppy assets

#### Scenario: Addon asset cleanup on disable
- **WHEN** an addon is disabled or unloaded
- **THEN** its asset tree is excluded from subsequent resource pack builds without corrupting base Croppy assets

#### Scenario: Computing SHA-1 checksum
- **WHEN** the resource pack compilation finishes successfully
- **THEN** the system generates an exact 40-character hexadecimal SHA-1 digest matching the ZIP archive contents

### Requirement: Embedded Resource Pack HTTP Server
The system SHALL provide an embedded HTTP server using standard Java 21 runtime facilities (`jdk.httpserver`) that binds to a configurable host and port, serving the active unified resource pack archive over HTTP with proper `Content-Type: application/zip`, `Content-Length`, and `ETag` headers.

#### Scenario: Serving the compiled pack over HTTP
- **WHEN** a client performs an HTTP `GET` request to the configured pack endpoint
- **THEN** the server streams the resource pack ZIP archive with status 200, matching content length, and an ETag header corresponding to the SHA-1 checksum

#### Scenario: Graceful shutdown without thread leaks
- **WHEN** the plugin is disabled or reloaded
- **THEN** the embedded HTTP server terminates immediately, closing listening sockets and releasing executor threads

### Requirement: In-Game Resource Pack Dispatch and Delivery
The system SHALL support dispatching the compiled companion resource pack to connected players using Paper's resource pack delivery API, prompting players with a configurable MiniMessage prompt.

#### Scenario: Auto-sending pack on player join
- **WHEN** a player joins the server and `resource-pack.delivery.auto-send-on-join` is enabled
- **THEN** the system automatically prompts the player to download the active resource pack URL with its SHA-1 hash

#### Scenario: Logging resource pack status telemetry
- **WHEN** a player accepts, successfully downloads, or declines the resource pack prompt
- **THEN** the system captures the event telemetry and logs diagnostic status without interrupting player gameplay

### Requirement: Neutral Starter Pack Assets and Vinery Isolation
The companion resource pack SHALL isolate viticulture and grape-specific assets (`plant/grape`, `purple_grapes`, `green_grapes`) into `.internal/resourcepack/` to maintain aesthetic neutrality, and SHALL supply dedicated textures and model geometries for all base starter-pack items (watering cans, fertilizers, tools, seeds, harvested produce, and miscellaneous culinary items) under `assets/croppy/models/item/` and `assets/croppy/textures/item/`.

#### Scenario: Isolating vinery assets from core pack
- **WHEN** the companion resource pack is inspected for theme neutrality
- **THEN** grape plant stages and grape item assets are relocated to `.internal/resourcepack/vinery/` and are absent from `resourcepack/assets/croppy/`

#### Scenario: Verifying dedicated custom assets for base items
- **WHEN** a client renders any registered base starter-pack item
- **THEN** the client resolves a custom pixel-art texture and model under `croppy:*` rather than falling back to vanilla hoes, sticks, or coal
