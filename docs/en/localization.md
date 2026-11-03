---
title: Localization & i18n
description: Complete guide to Croppy's BCP-47 locale bundles, fallback resolution, MiniMessage formatting, and Crowdin integration.
sidebar:
  order: 18
---

# Croppy Localization & Internationalization (i18n)

This document describes the design, architecture, key conventions, and workflow for Croppy's internationalization (i18n) system and its integration with Crowdin.

---

## 1. Architectural Overview

Croppy follows a strict **Hexagonal Architecture (Ports and Adapters)** to decouple domain logic from Minecraft/Paper APIs and external translation tooling:

```text
+--------------------------------------------------------+
|                      croppy-api                        |
|   Pure Java 21 contracts, zero external dependencies   |
|   * MessageKey (immutable semantic key record)         |
|   * LocaleRef (BCP-47 locale tag representation)       |
|   * TranslationProvider (SPI query port)               |
+--------------------------+-----------------------------+
                           | implemented by
+--------------------------v-----------------------------+
|                      croppy-core                       |
|   Domain engine & pure in-memory message catalog       |
|   * DefaultMessageCatalog (thread-safe, atomic swap)   |
|   * YamlLocaleLoader (SnakeYAML flattener)             |
|   * LocaleValidator & LocaleValidationReport          |
+--------------------------+-----------------------------+
                           | adapted by
+--------------------------v-----------------------------+
|                     croppy-paper                       |
|   Paper platform adapter (Paper 1.21.8+)               |
|   * PaperMessageService (MiniMessage & Adventure)      |
|   * Argument sanitization (prevents tag injection)     |
|   * Content localization override resolver             |
|   * Bundled resource bundles (en-US.yml, es-ES.yml)    |
+--------------------------------------------------------+
```

---

## 2. Deterministic Fallback Resolution

When resolving a message for an audience (such as a `Player`), the catalog evaluates the following 5-tier fallback chain:

1. **Exact Player Locale Tag**: Matches the player's Minecraft client locale (e.g. `es-MX`).
2. **Base Language Fallback**: If `es-MX` is missing the key, falls back to the base language catalog (e.g. `es-ES` or `es`).
3. **Configured Server Default Locale**: Configured via `messages.default-locale` in `config.yml` (defaults to `en-US`).
4. **Bundled Canonical Source Locale**: Canonical fallback `en-US` embedded in the plugin JAR.
5. **Safe Missing Key Token**: If the key is missing from all catalogs, returns `<missing:namespace:value>` without throwing exceptions or crashing the game tick. Missing keys are logged as warnings with rate-limiting to prevent log spam.

---

## 3. Key Naming Taxonomy & Structure

All message keys use lowercase dot-delimited paths under semantic categories:

| Prefix | Scope | Examples |
|---|---|---|
| `general.*` | Common plugin messages & prefixes | `general.prefix`, `general.no-permission`, `general.reload-success` |
| `command.*` | Command feedback, arguments, and errors | `command.give.received`, `command.list.category-header` |
| `inspector.*` | Crop Inspection tool card, states, & lines | `inspector.title`, `inspector.species`, `inspector.progress` |
| `watering.*` | Watering cans and hydration feedback | `watering.refilled`, `watering.empty` |
| `fertilizer.*` | Fertilizers and soil enrichment feedback | `fertilizer.applied`, `fertilizer.already-applied` |
| `station.*` | Crafting stations, temperature, and action bars | `station.temperature`, `station.empty-feedback` |
| `prompt.*` | Interactive chat prompts (e.g. temperature input) | `prompt.temperature-prompt`, `prompt.temperature-bounds` |
| `content.*` | Optional content definition overrides | `content.item.croppy.wheat_seeds.name` |

### Content Localization Overrides

Croppy allows translating items, recipes, or stations without modifying or duplicating content YAML definitions. If a locale file defines:

```yaml
content:
  item:
    croppy:
      wheat_seeds:
        name: "<yellow>Golden Wheat Seeds</yellow>"
        lore:
          - "<gray>Plant these seeds on tilled farmland.</gray>"
          - "<gold>Guaranteed superior harvest quality.</gold>"
```

`PaperMessageService#resolveItemDisplayName` and `resolveItemLore` will automatically prioritize the localized values over the default definition strings in `items/seeds/wheat_seeds.yml`.

---

## 4. Placeholders & MiniMessage Tag Injection Safety

Croppy uses **Kyori Adventure MiniMessage** for all player-facing text formatting:

```yaml
command:
  give:
    received: "<green>You received <yellow><amount>x</yellow> <item>.</green>"
```

### Argument Sanitization

Untrusted dynamic inputs (such as player names, unvalidated text, or external inputs) MUST be passed using `PaperMessageService.arg(key, value)` or `PaperMessageService.args(...)`.

These methods wrap values in `Placeholder.unparsed(key, stringValue)`, preventing malicious users from injecting formatting tags (e.g., `<click:run_command:/op>`).

When intentional rich formatting is required (such as pre-formatted sub-components or gradients), pass a pre-built Adventure `Component` into `PaperMessageService.arg(key, component)`.

---

## 5. Adding a New Language

### Step 1: Create the Translation File

Create a new file in `croppy-paper/src/main/resources/locales/<locale-tag>.yml` (e.g., `fr-FR.yml` or `de-DE.yml`).

### Step 2: Copy Keys from Canonical Source

Copy all keys from `croppy-paper/src/main/resources/locales/en-US.yml` and translate the values using MiniMessage syntax.

### Step 3: Verify Integrity Locally

Execute the automated locale validator:

```powershell
.\gradlew.bat test --tests "dev.darkblade.croppy.platform.paper.i18n.LocaleResourcesIntegrityTest"
```

The test validates:
- Syntax correctness of YAML mappings.
- Non-empty values for all entries.
- 100% key synchronization between secondary locales and `en-US.yml`.

---

## 6. Crowdin Integration

Croppy integrates with [Crowdin](https://crowdin.com) for crowd-sourced translations.

### Configuration (`crowdin.yml`)

The root `crowdin.yml` maps the canonical English source to target translations:

```yaml
project_id_env: CROWDIN_PROJECT_ID
api_token_env: CROWDIN_PERSONAL_TOKEN
base_path: "."
base_url: "https://api.crowdin.com"

preserve_hierarchy: true

files:
  - source: /croppy-paper/src/main/resources/locales/en-US.yml
    translation: /croppy-paper/src/main/resources/locales/%locale%.yml
    update_option: update_as_unapproved
```

### GitHub Actions CI/CD Synchronization

- **`.github/workflows/build.yml`**: Runs on push and PR to verify compilation, test suites, and locale integrity.
- **`.github/workflows/crowdin-sync.yml`**: Automatically uploads updated `en-US.yml` source strings to Crowdin and creates pull requests with newly translated community contributions.
- **Required Repository Secrets**:
  - `CROWDIN_PROJECT_ID`: The numeric project ID from Crowdin.
  - `CROWDIN_PERSONAL_TOKEN`: A Crowdin personal access token with project permissions.

### Runtime Independence

Croppy is completely independent of Crowdin at runtime. Crowdin is strictly a development and CI/CD service; the plugin requires zero network access to operate.
