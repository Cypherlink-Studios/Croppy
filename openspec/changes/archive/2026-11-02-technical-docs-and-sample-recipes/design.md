# Design

## Context

See `proposal.md` for background and motivation.

Croppy implements pure Java 21 YAML loaders in `croppy-core`:
- `YamlRecipeLoader`: Parses recipe metadata, inputs, outputs, processes, hints, and discovery rules.
- `YamlStationLoader`: Parses workstation blocks, interaction models, capacities, PacketEvents 3D virtual displays (`process_display` and `text_display`), and thermal properties.
- `YamlRuleLoader`: Parses cooking rules with compiled Pratt AST condition expressions (`process.finished == true and station.temperature > 120`).
- `YamlInteractionLoader`: Parses physical/sneaking click interactions, item filters, and action dispatchers.
- `YamlItemLoader`: Parses custom item definitions with Minecraft 1.21.4+ native Data Component `item_model`, CustomModelData, and PDC tags.
- `YamlFertilizerLoader`: Parses nutrient fertilizers (`SPEED`, `QUALITY`, `RETAINING`).

The documentation portal uses Astro + Starlight via `github-docs-loader` consuming Markdown files in `docs/en/` and `docs/es/`. Currently, these loaders lack technical YAML reference pages, and `croppy-paper` ships without default recipe configurations.

## Goals / Non-Goals

**Goals:**
- Provide complete technical YAML specification pages in English (`docs/en/`) and Spanish (`docs/es/`) for all core loaders.
- Document PacketEvents virtual 3D display layer transformations, billboard hologram templates, and placeholder formatting.
- Document Pratt AST expression grammar, available variables, comparison operators, and failure action triggers.
- Bundle 3 high-quality default recipes (`tomato_soup.yml`, `vegetable_stew.yml`, `fried_egg.yml`) and their corresponding food item definitions.
- Automatically unpack default recipes into `plugins/Croppy/recipes/` on first startup via `CroppyPlugin#setupRecipeDefinitions()`.
- Re-index sidebar navigation numbers from 1 to 18 across both documentation catalogs.

**Non-Goals:**
- Modifying or extending core YAML parser code in `croppy-core` (the domain loaders already support the complete schema).
- Generating custom textures or 3D block models in this change (addressed separately via the companion resource pack).

## Decisions

### 1. Dedicated Technical Reference Pages (Option A)
* **Choice**: Create dedicated pages (`recipes-configuration.md`, `workstations-configuration.md`, `rules-and-interactions.md`) instead of stuffing reference schemas into conceptual gameplay pages.
* **Rationale**: Maintains a clean separation between player-facing gameplay guides and server administrator configuration manuals, ensuring high readability and maintainability in Starlight.

### 2. Dual Language Synchronization (`en` and `es`)
* **Choice**: Author every new and updated documentation page in English (`defaultLocale`) and Spanish (`es`) with matching frontmatter (`title`, `description`, `sidebar.order`).
* **Rationale**: Required by the DarkBladeDev documentation portal and `plugin-docs-versioning` guidelines.

### 3. Default Recipe Sample Suite
* **Choice**: Bundle three representative recipes covering different stations and mechanics:
  1. `croppy:tomato_soup`: Multi-ingredient simmered soup in `croppy:cooking_pot` (2 `croppy:tomato` + 1 `minecraft:bowl` $\rightarrow$ 1 `croppy:tomato_soup`) with experimental discovery and hint.
  2. `croppy:vegetable_stew`: Multi-species stew in `croppy:cooking_pot` (1 `croppy:tomato` + 1 `minecraft:carrot` + 1 `minecraft:potato` + 1 `minecraft:bowl` $\rightarrow$ 1 `croppy:vegetable_stew`).
  3. `croppy:fried_egg`: Single-input searing dish in `croppy:skillet` (1 `minecraft:egg` $\rightarrow$ 1 `croppy:fried_egg`).
* **Rationale**: Gives server owners immediate playable content verifying both `cooking_pot` and `skillet` without requiring initial manual configuration.

### 4. Non-Destructive Resource Unpacking
* **Choice**: Use `saveResourceIfNotExists` in `CroppyPlugin#setupRecipeDefinitions()` and `setupItemDefinitions()`.
* **Rationale**: Ensures out-of-the-box readiness on a fresh installation while respecting administrator edits or deliberate deletions on subsequent server reboots.

## Risks / Trade-offs

* **[Risk] Sidebar order drift between language trees** $\rightarrow$ *Mitigation*: Renumber all 18 files simultaneously in `docs/en/` and `docs/es/` using an explicit mapping table.
* **[Risk] Missing ingredient/result item definitions** $\rightarrow$ *Mitigation*: Register corresponding items (`croppy:tomato_soup`, `croppy:vegetable_stew`, `croppy:fried_egg`) under `items/ingredients/` so RecipeRegistry and CompendiumMenu can resolve display icons cleanly.
