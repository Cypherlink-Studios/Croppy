# Design: Plugin Documentation Architecture and Metadata

## Context

Croppy is a modular agriculture, ingredient simulation, workstation, and cooking engine for Paper 1.21.8 servers. The DarkBladeDev documentation portal uses Astro + Starlight with a static content loader (`github-docs-loader.ts`) that clones or reads repositories at build time. To integrate into the portal catalog, Croppy requires a root `docs/metadata.yml` descriptor and bilingual documentation directories (`docs/en/` and `docs/es/`) with identical slug filenames.

See `proposal.md` for motivation and high-level scope.

## Goals / Non-Goals

**Goals:**
- Provide a valid, production-grade `docs/metadata.yml` conforming to DarkBladeDev's portal schema with English descriptions.
- Author 15 comprehensive documentation pages under `docs/en/` and 15 corresponding pages under `docs/es/` maintaining exact 1:1 slug parity.
- Focus strictly on server administration, configuration guides, and player gameplay mechanics.
- Migrate and retire the standalone `docs/localization.md` file.
- Use clean, standard ASCII diagrams and MiniMessage syntax in all guides.

**Non-Goals:**
- Developer API documentation (`developer-api.md`), SPI contracts, and internal Java architecture guides (deferred to a future developer-centric documentation phase).
- Changes to plugin runtime code, Java sources, or Gradle build configurations.
- Hosting or modifying the Astro/Starlight web portal repository itself.

## Decisions

### 1. Flat File Structure with Frontmatter Sidebar Ordering
* **Choice**: Place all 15 markdown files directly within `docs/en/` and `docs/es/` (e.g., `docs/en/growth-and-environment.md`) rather than nested subfolders (`docs/en/agriculture/growth.md`).
* **Rationale**: Sibling plugins in the DarkBladeDev ecosystem (`CraftingEngine`, `PlayerFurnaces`, `CinematicEngine`) use a flat structure with frontmatter `sidebar.order: <number>`. This guarantees consistent URL route generation (`/[lang]/docs/croppy/[page]/`) and prevents slug divergence between languages.
* **Alternatives considered**: Nested folders. Rejected because nested paths introduce routing depth variances and increase complexity for Starlight sidebar generation.

### 2. English as the Canonical Baseline for `metadata.yml`
* **Choice**: Write all top-level descriptive fields in `docs/metadata.yml` (`description`, `folia.notes`, `versions.changelog`) in English.
* **Rationale**: Standardizes metadata across the DarkBladeDev multi-project catalog while documentation pages themselves are localized in `docs/en/` and `docs/es/`.

### 3. Comprehensive 15-Page Scope
The documentation suite will cover all player and administrator systems currently implemented in Croppy:
1. `index.md` — Plugin overview, key highlights, core gameplay loop.
2. `getting-started.md` — Installation, PacketEvents dependency, first crop & harvest.
3. `configuration.md` — `config.yml` breakdown, persistence backends (SQLite/PDC), tick intervals.
4. `commands-and-permissions.md` — Complete Cloud 2 command hierarchy and permission nodes.
5. `crops-and-species.md` — Crop definition schemas (`crops/*.yml`), lifecycle stages, requirements.
6. `growth-and-environment.md` — Growth simulation, light, temperature, hydration, greenhouse rules.
7. `soil-and-fertilizers.md` — Soil nutrients (N-P-K), fertilizer types, application mechanics.
8. `watering-mechanics.md` — Watering cans, tiers, moisture evaporation, refill sources.
9. `workstations-and-cooking.md` — Physical stations (`cooking_pot`, `skillet`), interactions, cooking rules.
10. `heat-sources-and-fuel.md` — Stoves, fuel properties, heat dissipation, thermal management.
11. `recipe-discovery.md` — Discovery engine, knowledge states (`LOCKED`, `HINTED`, `UNLOCKED`), unlock criteria.
12. `culinary-compendium.md` — In-game recipe book, category navigation, cookbook GUI.
13. `gui-menus.md` — Configurable 9xN matrix inventories, slot mappings, actions, template items.
14. `items-and-tools.md` — Custom items (thermometer, inspector, recipe book), PDC tags, item registration.
15. `localization.md` — BCP-47 locale bundles, 5-tier fallback chain, Crowdin integration.

### 4. Diagram and Text Styling
* **Choice**: Fenced text blocks using standard ASCII characters (`+`, `-`, `|`, `-->`, `*`).
* **Rationale**: In accordance with project conventions, Unicode box-drawing characters often render with variable widths in Starlight code blocks across platforms.

## Risks / Trade-offs

- **[Risk] Missing slug parity across locales** → *Mitigation*: Ensure every single file created in `docs/en/` has an exact match in `docs/es/`. An automated shell check will verify identical filename sets.
- **[Risk] Inaccurate configuration examples** → *Mitigation*: Directly ground all example YAML snippets and item names in the bundled defaults from `croppy-paper/src/main/resources`.
- **[Risk] Breaking legacy links to `docs/localization.md`** → *Mitigation*: Both `docs/en/localization.md` and `docs/es/localization.md` preserve the complete content and enhance it with the latest Crowdin workflow additions.
