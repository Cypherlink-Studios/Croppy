# Design: Root Project README Architecture

## Context

Croppy is a modern Paper 1.21.8+ agriculture and culinary simulation engine built using Hexagonal Architecture. While detailed developer and administrator guides exist under `docs/en/` and companion pack details reside in `resourcepack/README.md`, the repository root lacks a primary entry-point `README.md`. Furthermore, legal policy guidelines have been formalized in `.internal/docs/Croppy-Licensing-Agent-Brief.md` establishing dual-licensing intentions (`GPL-3.0-or-later` for code and `CC BY-NC-SA 4.0` for assets).

See `proposal.md` for motivation and high-level scope.

## Goals / Non-Goals

**Goals:**
- Provide a clean, engaging GitHub landing page summarizing Croppy's product value, architecture, use cases, and gameplay mechanics.
- Ensure 100% technical fidelity with the actual codebase: Cloud 2.0 command syntax, Chunk PDC default storage, PacketEvents virtual displays, and survival walkthrough.
- Faithfully reflect the licensing directives without overpromising legal status or inventing entities.
- Ensure all relative document links (`docs/en/*`, `resourcepack/*`) are verified and functional.

**Non-Goals:**
- Modifying Java code or altering Gradle configurations.
- Generating the full legal documentation suite (`LICENSE`, `LICENSING.md`, `COMMERCIAL-LICENSE.md`, `COPYRIGHT.md`, `CLA.md`, `THIRD-PARTY-NOTICES.md`) in this change; those belong to a dedicated licensing change.

## Decisions

### 1. Structure and Section Sequencing
The README is structured into logical thematic blocks:
1. Header & Quick Metadata Badges (Java 21, Paper 1.21.8+, Hexagonal Architecture, Licenses).
2. Product Definition & Core Highlights (Plain ASCII diagram highlighting agronomy, chemistry, workstations, and compendium).
3. Common Server Use Cases (SMP, RPG/Economy, Towny, Modpack-like experience).
4. Architecture & Engineering Principles (Zero Bukkit in Core, Ghost-free packet rendering, Chunk PDC persistence).
5. Quick Start & Survival Walkthrough (Installation, player & admin commands, step-by-step planting, watering, and cooking).
6. Planned Integrations Roadmap (WorldGuard, Lands, Towny, Vault, PlaceholderAPI).
7. Documentation, Resource Pack, and Licensing Attributions.

*Alternative Considered*: A minimal technical README pointing immediately to external docs.  
*Rationale*: A comprehensive self-contained README engages prospective server owners, developers, and players directly on GitHub without requiring external navigation.

### 2. Verified Command and Feature Alignment
All command examples use verified Cloud 2.0 signatures from `CroppyCommands.java`:
- `/croppy item give <category> <item_id> [quantity]`: uses valid categories (`seeds`, `watering_cans`, `tools`, `fertilizers`, `stations`, `misc`) and registered keys (`wheat_seeds`, `copper_can`, `inspection_tool`, `recipe_book`).
- `/croppy recipes`: the actual compendium command (replacing nonexistent `/croppy compendium`).
- Default storage is accurately presented as Chunk PersistentDataContainer (PDC) with optional SQLite.

### 3. Licensing Precision
The licensing section adheres strictly to `.internal/docs/Croppy-Licensing-Agent-Brief.md`:
- Code: `GNU General Public License v3.0 or later (GPL-3.0-or-later)`.
- Commercial pathway: explicitly noted as an alternative route with references to planned `LICENSING.md`.
- Assets: `CC BY-NC-SA 4.0` linking to `resourcepack/LICENSE`.

## Risks / Trade-offs

- **[Risk] Broken relative links to future or moved documents**  
  → *Mitigation*: Only link to existing verified documents (`docs/en/getting-started.md`, `docs/en/crops-and-species.md`, `docs/en/soil-and-fertilizers.md`, `docs/en/workstations-and-cooking.md`, `docs/en/localization.md`, `resourcepack/LICENSE`). Placeholder links to planned licensing files (`LICENSE`, `LICENSING.md`, `COPYRIGHT.md`) will resolve as that legal rollout proceeds.
- **[Risk] Command signature drift as new features are added**  
  → *Mitigation*: Keep command examples focused on foundational mechanics (seeds, watering can, inspector, recipes GUI) covered by unit/integration tests.
