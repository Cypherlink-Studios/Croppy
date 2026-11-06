# Design: Repository Legal & Licensing Documentation Suite

## Context

Croppy is a modern Minecraft agriculture, ingredients, processing, and cooking simulation engine. The project's legal direction has been outlined in `.internal/docs/Croppy-Licensing-Agent-Brief.md`. The owner has selected a dual-licensing path:
1. Public codebase licensed under GNU General Public License v3.0 or later (`GPL-3.0-or-later`).
2. An alternative commercial licensing pathway reserved for commercial extensions and proprietary permissions.
3. Companion visual/audio assets under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (`CC BY-NC-SA 4.0`).

This design specifies the creation of seven cohesive legal and governance documents referenced by `README.md`.

## Goals / Non-Goals

**Goals:**
- Provide full, canonical GPLv3 text in root `LICENSE`.
- Author `LICENSING.md` as an authoritative, plain-language guide detailing GPLv3 rights/obligations, the commercial alternative, addon policies, and a practical FAQ.
- Provide `COMMERCIAL-LICENSE.md` and `CLA.md` clearly marked as non-operational drafts/templates requiring formal legal review.
- Document copyright notices in `COPYRIGHT.md` using verified facts and explicit placeholders where legal personhood is unconfirmed.
- Establish contribution guidelines in `CONTRIBUTING.md` regarding originality, third-party code, and future CLA readiness.
- Compile an exhaustive inventory in `THIRD-PARTY-NOTICES.md` covering all direct Gradle dependencies and companion assets.

**Non-Goals:**
- Global modification of source code headers or SPDX tags across existing Java files.
- Providing binding legal counsel or conclusively resolving international software copyright status.
- Inventing commercial fee schedules, corporate entities, jurisdiction choices, or signing platforms.

## Decisions

### 1. Document Taxonomy & Interlinking
All seven documents will reside at the repository root and link to each other using relative Markdown paths:
- `LICENSE`: Canonical unmodified text of GNU GPLv3.
- `LICENSING.md`: Central policy hub linking to `LICENSE`, `COMMERCIAL-LICENSE.md`, `COPYRIGHT.md`, `CONTRIBUTING.md`, and `THIRD-PARTY-NOTICES.md`.
- `COMMERCIAL-LICENSE.md`: Draft commercial terms template with clear draft banners and placeholder variables (`[Licensor Name]`, `[Contact Email]`, `[Territory/Jurisdiction]`).
- `COPYRIGHT.md`: Explicit attribution for Cypherlink Studios & DarkBladeDev with legal entity reservations.
- `CONTRIBUTING.md`: PR workflow, coding standards cross-reference to `AGENTS.md`, and originality representations.
- `CLA.md`: Individual/entity contributor agreement draft preserving contributor copyright while licensing the project for dual licensing.
- `THIRD-PARTY-NOTICES.md`: Detailed attribution for all dependencies in `build.gradle.kts` files.

### 2. Precise Treatment of Addons & Technical Architecture
Following section 4 of the legal brief:
- The technical separation into API contracts (`croppy-api`), implementation (`croppy-core`), and platform adapters (`croppy-paper`) does not automatically determine legal independence.
- Official proprietary addons may be offered via commercial license where the rights holder has sufficient authority.
- Third-party developers cannot be given blanket assurances that proprietary addons linking to Croppy comply with GPLv3; they must independently assess their licensing obligations.

### 3. Server Use vs. Conveyance Clarification
GPLv3 FAQ explicitly distinguishes between running software on a server (SaaS/network play) and conveying binaries. `LICENSING.md` will clarify that operating Croppy on a public Minecraft server does not trigger source code distribution obligations (distinguishing GPLv3 from AGPLv3).

## Risks / Trade-offs

- **[Risk] Users mistake draft documents for legally binding active agreements**  
  → *Mitigation*: Prominent warning banners at the top of `COMMERCIAL-LICENSE.md` and `CLA.md` stating they are non-effective drafts subject to legal review.
- **[Risk] Invented legal identities**  
  → *Mitigation*: Strictly avoid creating fictitious corporate entities, addresses, or jurisdictions. Use clear bracketed placeholders.
