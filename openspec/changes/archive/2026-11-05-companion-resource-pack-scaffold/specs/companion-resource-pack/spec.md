# Spec Delta: companion-resource-pack

## Purpose

Provides an official Minecraft Java 1.21.8 companion resource pack (pack_format 64) for Croppy, managing item model definitions, negative-space GUI fonts, spatial audio, and asset quality validation.

## ADDED Requirements

### Requirement: Canonical 1.21.8 Pack Metadata
The companion resource pack SHALL declare root configuration in `pack.mcmeta` specifying integer `pack_format: 64` and `supported_formats` bounds of 63 to 64. It MUST NOT use decimal or floating format specifiers.

#### Scenario: Validating pack format
- **WHEN** a Minecraft 1.21.8 client inspects `pack.mcmeta`
- **THEN** the client recognizes the pack format as 64 without compatibility warnings

### Requirement: Namespaced Asset Hierarchy and Path Compliance
All resource pack assets SHALL be contained under `assets/croppy/`. Every directory and filename within the assets tree MUST strictly adhere to the lowercase regex `^[a-z0-9_.-]+$`.

#### Scenario: Auditing asset paths
- **WHEN** the asset directory tree is traversed
- **THEN** all path segments contain only lowercase alphanumeric characters, dots, underscores, and dashes

### Requirement: Modern Item Model Definitions
The resource pack SHALL declare custom item models inside `assets/croppy/items/<item>.json` utilizing modern dispatch trees (`minecraft:model`, `minecraft:select`, `minecraft:range_dispatch`). Item models MUST NOT declare legacy `overrides` arrays in `models/item/`.

#### Scenario: Resolving item definition for custom items
- **WHEN** a player holds an item stack whose `item_model` component points to `croppy:recipe_book`
- **THEN** the client resolves the model geometry via `assets/croppy/items/recipe_book.json`

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
