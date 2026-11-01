# Proposal: Plugin Documentation Architecture and Metadata

## Why

Croppy has achieved milestone 0.2.0-SNAPSHOT with comprehensive simulation features across agriculture, nutrient mechanics, cooking workstations, heat generation, recipe discovery, and localization. However, the repository lacks public documentation structured for the DarkBladeDev documentation portal (`github-docs-loader` in Astro + Starlight), leaving users and server administrators without formal guides for configuration, administration, and gameplay mechanics.

Setting up the official documentation architecture now ensures that the DarkBladeDev documentation portal can automatically ingest, cache, and serve multi-version, multi-language documentation with exact slug parity between English and Spanish.

## What Changes

- Add `docs/metadata.yml` following the DarkBladeDev `github-docs-loader` specification with all descriptions in English.
- Establish bilingual documentation directories `docs/en/` and `docs/es/` with 15 focused documents maintaining strict 1:1 slug parity.
- Focus the documentation scope on configuration, administration, and player mechanics (omitting developer API documentation for this initial phase).
- Migrate the existing standalone `docs/localization.md` into the bilingual structure (`docs/en/localization.md` and `docs/es/localization.md`).
- Validate compatibility with Starlight frontmatter schema (`title`, `description`, `sidebar.order`).

## Capabilities

### New Capabilities

*(None - documentation and portal metadata change only; `skip_specs: true` enabled).*

### Modified Capabilities

*(None - no behavioral domain contracts or simulation rules are modified).*

## Impact

- **Filesystem**: Creates `docs/metadata.yml`, `docs/en/*.md` (15 files), and `docs/es/*.md` (15 files). Removes the legacy unstructured `docs/localization.md`.
- **Portal Ingestion**: Enables automated parsing and building by `github-docs-loader.ts` on the DarkBladeDev website.
- **Codebase / Runtime**: Zero impact on Gradle modules (`croppy-api`, `croppy-core`, `croppy-paper`) or game server performance.
