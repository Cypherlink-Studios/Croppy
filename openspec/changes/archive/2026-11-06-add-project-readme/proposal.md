# Proposal: Add Project Root README

## Why

The Croppy repository currently lacks a root `README.md` to introduce the project, define its value proposition, and orient administrators, players, and developers. As Croppy evolves through version `0.5.3` with deep agricultural simulations, multi-block crop habits, physical workstations, and client companion resources, a comprehensive, accurate, and professional entry-point document is essential.

## What Changes

- Create a root `README.md` in clear, technical, and modern English.
- Articulate Croppy's identity as a modular simulation engine (agriculture, soil nutrients, cooking workstations, and culinary compendium).
- Document system requirements (Java 21, Paper 1.21.8+, PacketEvents 2.14.0+, and the Croppy Companion Resource Pack format 64).
- Provide accurate, verified Cloud 2.0 command syntax (`/croppy item give`, `/croppy recipes`, `/croppy inspect`, etc.).
- Detail the survival gameplay walkthrough from soil hydration through harvest regrowth and holographic workstation cooking.
- Establish architectural transparency (Hexagonal Ports & Adapters, Chunk PDC binary persistence, PacketEvents virtual displays).
- Align licensing declarations with the project legal brief: code under `GPL-3.0-or-later`, companion assets under `CC BY-NC-SA 4.0`, and cross-reference planned licensing documentation (`LICENSE`, `LICENSING.md`, `COMMERCIAL-LICENSE.md`, `COPYRIGHT.md`, `THIRD-PARTY-NOTICES.md`).

## Capabilities

### New Capabilities
*(None - documentation only change. Spec updates skipped via `skip_specs: true` in `.openspec.yaml`)*.

### Modified Capabilities
*(None - no behavioral requirements or API contracts are being modified)*.

## Impact

- **Code/APIs**: Zero impact on Java source code or runtime behavior.
- **Documentation**: Adds root `README.md` linking to existing sub-guides under `docs/en/` and `resourcepack/`.
- **Packaging & Repository**: Provides GitHub landing page presentation, badges, and accurate project attribution.
